package com.engitano.fs2pubsub

import scala.concurrent.ExecutionContext
import cats.effect._
import cats.effect.implicits._
import com.google.api.pubsub._
import com.google.auth.Credentials
import fs2._
import io.grpc.auth.MoreCallCredentials
import org.scalatest.{Matchers, WordSpec}

case class ResponseMessage()

class E2eSpec extends WordSpec with Matchers {

  import HasAckId._
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  "The Generated clients" should {
    "be able to read and write to PubSub" in {

      implicit val timer = IO.timer(ExecutionContext.global)

      implicit val intSerializer = Serializer.from[IO, Int](i => BigInt(i).toByteArray)
      implicit val intDeserializer = Deserializer.from[IO, Int](b => BigInt(b).toInt)
      val cfg = GrpcPubsubConfig.local("my-test-project", 8538)

      def publisher[F[_]: ConcurrentEffect](implicit T: ToPubSubMessage[F, Int]) =
        Publisher.stream[F,Int]("test-topic", cfg)(Stream.emits(0 to 11000))

      def subscriber[F[_]: ConcurrentEffect](implicit T: FromPubSubMessage[F, Int]) =
        Subscriber.stream[F,Int]("test-sub", cfg)(r => r)

      def run[F[_]](implicit E: ConcurrentEffect[F], T: ToPubSubMessage[F, Int], F: FromPubSubMessage[F, Int]) =
        subscriber[F]
          .concurrently(publisher)

      run[IO]
        .mapAsync(1)(r => IO.pure(println(r.wrapped)))
        .compile.toList.attempt.unsafeRunSync() match {
        case Right(v) => println(v)
        case Left(value) => throw value
      }
    }
  }

  def identity[F[_]](sr: Stream[F, ReceivedMessage]): Stream[F, ReceivedMessage] = sr
}
