/*
 * Copyright 2019 Engitano
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.engitano.fs2pubsub

import cats.Functor
import cats.implicits._
import cats.effect.{ConcurrentEffect, Sync}
import com.google.api.pubsub.{ReceivedMessage, StreamingPullRequest, StreamingPullResponse, SubscriberGrpc}
import fs2.{Pipe, Stream}
import fs2.concurrent.Queue
import io.grpc._
import org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall
import org.lyranthe.fs2_grpc.java_runtime.syntax.all._

case object EmptyPubsubMessageException extends Exception

case class WithAckId[A](payload: A, ackId: String) {
  def map[B](f: A => B): WithAckId[B] = this.copy(payload = f(this.payload))

  def mapW[B](f: WithAckId[A] => B) : WithAckId[B] = this.copy(payload = f(this))

  def peek(f: A => Unit): WithAckId[A] = {
    f(payload)
    this
  }
}

object WithAckId {

  implicit def flatMapForWithAckId: Functor[WithAckId] = new Functor[WithAckId] {
    override def map[A, B](fa: WithAckId[A])(f: A => B): WithAckId[B] = fa.map(f)
  }
}

trait HasAckId[T] {
  def getAckId(t: T): String
}

object HasAckId {

  def apply[T](implicit haid: HasAckId[T]): HasAckId[T] = haid

  implicit def receivedMessageHasAckId: HasAckId[ReceivedMessage] =
    new HasAckId[ReceivedMessage] {
    override def getAckId(t: ReceivedMessage): String = t.ackId
  }

  implicit def withAckIdHasAckId[T]: HasAckId[WithAckId[T]] =
    new HasAckId[WithAckId[T]] {
    override def getAckId(t: WithAckId[T]): String = t.ackId
  }

  implicit class HasAckIdOps[T](t: T) {
    def ackId(implicit h: HasAckId[T]): String = h.getAckId(t)
  }
}

trait LowPriorityDeserializerImplicits {
  implicit def forByteArrayDeserializer[F[_] : Deserializer[?[_], Array[Byte]]](implicit S: Sync[F]): Deserializer[F, Array[Byte]] =
    new Deserializer[F, Array[Byte]] {
    override def deserialize(b: Option[Array[Byte]]): F[Array[Byte]] = b match {
      case Some(b) => S.delay(b)
      case None => S.raiseError(EmptyPubsubMessageException)
    }
  }
}

object Deserializer {
  def apply[F[_], T](implicit d: Deserializer[F, T]): Deserializer[F, T] = d

  def from[F[_], T](f: Array[Byte] => T)(implicit S: Sync[F]): Deserializer[F, T] =
    new Deserializer[F, T] {
    override def deserialize(b: Option[Array[Byte]]): F[T] = b match {
      case Some(b) => S.delay(f(b))
      case None => S.raiseError(EmptyPubsubMessageException)
    }
  }
}

trait Deserializer[F[_], T] {
  def deserialize(b: Option[Array[Byte]]): F[T]
}

trait FromPubSubMessage[F[_], T] {
  def from(rm: ReceivedMessage): F[T]
}


trait FromPubSubMessageLowPriorityImplicits {
  implicit def idFromPubsubMessage[F[_]](implicit S: Sync[F]): FromPubSubMessage[F, ReceivedMessage] =
    new FromPubSubMessage[F, ReceivedMessage] {
      override def from(rm: ReceivedMessage): F[ReceivedMessage] = S.delay(rm)
    }

  implicit def deserializerFromPubsubMessage[F[_], T](implicit ds: Deserializer[F, T]): FromPubSubMessage[F, T] =
    new FromPubSubMessage[F, T] {
      override def from(rm: ReceivedMessage): F[T] = ds.deserialize(rm.message.map(_.data.toByteArray))
    }
}

object FromPubSubMessage extends FromPubSubMessageLowPriorityImplicits {
  def apply[F[_], T](implicit fpm: FromPubSubMessage[F, T]): FromPubSubMessage[F, T] = fpm
}

object Subscriber {
  import HasAckId._

  private val ACK_DEADLINE_SECONDS = 10

  def stream[F[_],A](subscription: String, cfg: GrpcPubsubConfig) : PubSubStream[F, A] =
    PubSubStream[F, A](cfg.host, cfg.subscriptionName(subscription), cfg)

  case class PubSubStream[F[_],A] private[Subscriber](host: String, subscription: String, cfg: GrpcPubsubConfig){
    def apply[B: HasAckId](pipe: Pipe[F,WithAckId[A], B])
                                (implicit S: ConcurrentEffect[F], FP: FromPubSubMessage[F, A]): Stream[F, B] = {
      cfg.channelBuilder
        .stream[F].flatMap[F, B] { channel =>

        Stream.eval(Queue.unbounded[F, StreamingPullRequest]).flatMap[F, B] { requestQueue =>
          def queueNext(req: StreamingPullRequest): F[Unit] = {
            requestQueue.enqueue1(req).as(())
          }

          val kickOff = Stream.eval(queueNext(StreamingPullRequest(subscription, Seq(), Seq(), Seq(), ACK_DEADLINE_SECONDS)))
          val doPull = streamingPull(channel, cfg.callOps, requestQueue.dequeue, new Metadata()).evalMap(deserialize)

          for {
            chunks <- kickOff >> doPull
            emit <- chunks.foldLeft[Stream[F, WithAckId[A]]](Stream.empty)((s, n) => s ++ Stream.emit(n)).through(pipe)
            emitAndCommit <- Stream.emit(emit) ++ Stream.eval_(queueNext(StreamingPullRequest(ackIds = Seq(emit.ackId))))
          } yield emitAndCommit
        }
      }
    }

    private def deserialize(spr: StreamingPullResponse)
                           (implicit S: ConcurrentEffect[F], FP: FromPubSubMessage[F, A]) =
      spr.receivedMessages.toList.traverse[F, WithAckId[A]](r => FP.from(r).map(t => WithAckId(t, r.ackId)))

    private def streamingPull[F[_]: ConcurrentEffect](
                                                       channel: Channel,
                                                       callOptions: CallOptions,
                                                       request: Stream[F, com.google.api.pubsub.StreamingPullRequest],
                                                       clientHeaders: Metadata): Stream[F, StreamingPullResponse] = {
      Stream.eval(Fs2ClientCall[F](channel, SubscriberGrpc.METHOD_STREAMING_PULL, callOptions))
        .flatMap(_.streamingToStreamingCall(request, clientHeaders))
    }
  }
}
