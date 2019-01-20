package com.engitano.fs2pubsub

import cats.{Applicative, Functor}
import cats.implicits._
import cats.effect.{ConcurrentEffect, Sync}
import com.google.api.pubsub.{PublishRequest, PublishResponse, PublisherGrpc, PubsubMessage}
import com.google.protobuf.ByteString
import io.grpc._
import fs2.Stream
import org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall
import org.lyranthe.fs2_grpc.java_runtime.syntax.all._

trait LowPrioritySerializerImplicits {
  implicit def fromByteArraySerializer[F[_]](implicit A: Applicative[F]): Serializer[F, Array[Byte]] =
    new Serializer[F, Array[Byte]] {
      override def serialize(t: Array[Byte]): F[Array[Byte]] = A.pure(t)
    }
}

object Serializer extends LowPrioritySerializerImplicits{
  def apply[F[_], T](implicit s: Serializer[F,T]): Serializer[F,T] = s

  def from[F[_], T](f: T => Array[Byte])(implicit S: Sync[F]) = new Serializer[F, T] {
    override def serialize(t: T): F[Array[Byte]] = S.delay(f(t))
  }

  def fromF[F[_], T](f: T => F[Array[Byte]]) = new Serializer[F, T] {
    override def serialize(t: T): F[Array[Byte]] = f(t)
  }
}

trait Serializer[F[_], T] {
  def serialize(t: T): F[Array[Byte]]
}

trait ToPubSubMessage[F[_], T] {
  def to(t: T): F[PubsubMessage]
}

trait LowPriorityToPubSubMessageImplicits {

  implicit def fromPubSubMessage[F[_]](implicit A: Sync[F]): ToPubSubMessage[F, PubsubMessage] =
    new ToPubSubMessage[F, PubsubMessage] {
      override def to(t: PubsubMessage): F[PubsubMessage] = A.delay(t)
    }

  implicit def fromSerializerFromPubSubMessage[F[_] : Functor, T](implicit ser: Serializer[F, T]): ToPubSubMessage[F, T] =
    new ToPubSubMessage[F, T] {
      override def to(t: T): F[PubsubMessage] = ser.serialize(t).map(m => PubsubMessage(ByteString.copyFrom(m)))
    }
}

object ToPubSubMessage extends LowPriorityToPubSubMessageImplicits{
  def apply[F[_], T](implicit psm: ToPubSubMessage[F,T]): ToPubSubMessage[F,T] = psm

}

object Publisher {

  private val MAX_PUBSUB_MESSAGE_BATCH = 1000

  def stream[F[_]: ConcurrentEffect, T](topic: String, cfg: GrpcPubsubConfig)
                                       (s: Stream[F, T])(implicit psm: ToPubSubMessage[F, T]) = {
    ManagedChannelBuilder
      .forTarget(cfg.host)
      .stream[F] flatMap { channelBuilder =>
      s
        .chunkLimit(MAX_PUBSUB_MESSAGE_BATCH)
        .evalMap(c => c.traverse(psm.to))
        .evalMap(p => publish(channelBuilder, cfg.callOps, PublishRequest(cfg.topicName(topic), p.toList), new Metadata()))
        .flatMap(pr => Stream.emits(pr.messageIds))
    }
  }

  private def publish[F[_]: ConcurrentEffect](
                             channel: Channel,
                             callOptions: CallOptions,
                             request: PublishRequest,
                             clientHeaders: Metadata): F[PublishResponse] =
    Fs2ClientCall[F](channel, PublisherGrpc.METHOD_PUBLISH, callOptions)
      .flatMap(_.unaryToUnaryCall(request, clientHeaders))
}
