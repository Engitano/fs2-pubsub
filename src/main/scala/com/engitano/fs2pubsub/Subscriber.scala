package com.engitano.fs2pubsub

import cats.Functor
import cats.implicits._
import cats.effect.{ConcurrentEffect, Sync}
import com.google.api.pubsub.{ReceivedMessage, StreamingPullRequest, StreamingPullResponse, SubscriberGrpc}
import fs2.Stream
import fs2.concurrent.Queue
import io.grpc._
import org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall
import org.lyranthe.fs2_grpc.java_runtime.syntax.all._

case object EmptyPubsubMessageException extends Exception

case class WithAckId[A](wrapped: A, ackId: String) {
  def map[B](f: A => B): WithAckId[B] = this.copy(wrapped = f(this.wrapped))

  def mapW[B](f: WithAckId[A] => B) : WithAckId[B] = this.copy(wrapped = f(this))
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

  def `for`[T](f: T => String): HasAckId[T] =
    new HasAckId[T] {
    override def getAckId(t: T): String = f(t)
  }

  implicit def receivedMessageHasAckId: HasAckId[ReceivedMessage] =
    new HasAckId[ReceivedMessage] {
    override def getAckId(t: ReceivedMessage): String = t.ackId
  }

  implicit def withAckIdHasAckId[T]: HasAckId[WithAckId[T]] =
    new HasAckId[WithAckId[T]] {
    override def getAckId(t: WithAckId[T]): String = t.ackId
  }

  implicit class HasAckIdOps[T](t: T) {
    def ackId(implicit h: HasAckId[T]) = h.getAckId(t)
  }
}

trait LowPriorityDeserializerImplicits {
  implicit def forByteArrayDeserializer[F[_]](implicit S: Sync[F], D: Deserializer[F, Array[Byte]]): Deserializer[F, Array[Byte]] =
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


  def stream[F[_],A](subscription: String, cfg: GrpcPubsubConfig) : PubSubStream[F, A] =
    PubSubStream[F, A](cfg.host, cfg.subscriptionName(subscription), cfg.callOps)

  case class PubSubStream[F[_],A] private[Subscriber](host: String, subscription: String, callOps: CallOptions){
    def apply[B: HasAckId](f: Stream[F,WithAckId[A]] => Stream[F, B])
                                (implicit S: ConcurrentEffect[F], FP: FromPubSubMessage[F, A]): Stream[F, B] = {
      ManagedChannelBuilder
        .forTarget(host)
        .stream[F] flatMap { channel =>

        Stream.eval(Queue.unbounded[F, StreamingPullRequest]) flatMap { requestQueue =>
          def queueNext(req: StreamingPullRequest): F[Unit] = {
            requestQueue.enqueue1(req).as(())
          }

          for {
            _ <- Stream.eval(queueNext(StreamingPullRequest(subscription, Seq(), Seq(), Seq(), 10)))
            resp <- streamingPull(channel, callOps, requestQueue.dequeue, new Metadata())
              .evalMap(_.receivedMessages.toList.traverse[F,WithAckId[A]](r => FP.from(r).map(t => WithAckId(t, r.ackId))))
              .flatMap(_.foldLeft[Stream[F,WithAckId[A]]](Stream.empty)((s,n) => s ++ Stream.emit(n)))
            st <- f(Stream.emit(resp))
            r <- Stream.emit(st) ++ Stream.eval_(queueNext(StreamingPullRequest(ackIds = Seq(st.ackId))))
          } yield r
        }
      }
    }

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
