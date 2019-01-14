package com.engitano.fs2pubsub

import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

import cats.{FlatMap, Functor, Monad}
import cats.implicits._

import scala.concurrent.ExecutionContext
import cats.effect._
import cats.effect.implicits._
import com.google.api.pubsub._
import com.google.auth.oauth2.GoogleCredentials
import fs2._
import fs2.concurrent.Queue
import org.lyranthe.fs2_grpc.java_runtime.syntax.all._
import io.grpc._
import io.grpc.auth.MoreCallCredentials
import org.scalatest.{Matchers, WordSpec}
import scalapb.descriptors.ScalaType.ByteString

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

case class ResponseMessage()

class E2eSpec extends WordSpec with Matchers {

  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  "The Generated clients" should {
    "be able to read and write to PubSub" in {

      def managedChannelStream[F[_] : Sync]: Stream[F, ManagedChannel] =
        ManagedChannelBuilder
          .forTarget("pubsub.googleapis.com")
          .stream[F]

      def buildTopic[F[_] : Functor](publisher: PublisherFs2Grpc[F]): F[String] =
        publisher.createTopic(Topic("projects/gcp-playground-228403/topics/test-topic"), new Metadata()).map(_.name)

      def deleteTopic[F[_] : Functor](publisher: PublisherFs2Grpc[F])(topic: String): F[Unit] =
        publisher.deleteTopic(DeleteTopicRequest(topic), new Metadata()).as(())

      def createSubscription[F[_]: Functor](sub: SubscriberFs2Grpc[F])(topic: String): F[String] = {
        sub.createSubscription(Subscription("projects/gcp-playground-228403/subscriptions/test-sub", topic, None, 10), new Metadata()).map(_.name)
      }

      def deleteSubscription[F[_]: Functor](sub: SubscriberFs2Grpc[F])(subsc: String): F[Unit] = {
        sub.deleteSubscription(DeleteSubscriptionRequest(subsc), new Metadata()).as(())
      }

      def produceMessages[F[_]: Functor](publisher: PublisherFs2Grpc[F])(topic: String, msgs: Seq[String]): F[Unit] = {
        val pubsubMsgs = msgs.map(s => PubsubMessage(com.google.protobuf.ByteString.copyFrom(s, Charset.defaultCharset())))
        publisher.publish(PublishRequest(topic, pubsubMsgs), new Metadata()).as(())
      }

      val creds = GoogleCredentials.getApplicationDefault()

      implicit val timer = IO.timer(ExecutionContext.global)

      def run[F[_]](implicit E: ConcurrentEffect[F]) = for {
        managedChannel <- managedChannelStream[F]
        publisher = PublisherFs2Grpc.stub[F](managedChannel, CallOptions.DEFAULT.withCallCredentials(MoreCallCredentials.from(creds)))
        subscriber = SubscriberFs2Grpc.stub[F](managedChannel, CallOptions.DEFAULT.withCallCredentials(MoreCallCredentials.from(creds)))
//        topic <- Stream.eval(buildTopic[F](publisher))
//        subsc <- Stream.eval(createSubscription[F](subscriber)(topic))
        _ <-  Stream.eval(produceMessages(publisher)("projects/gcp-playground-228403/topics/test-topic", 1 to 1000 map { _.toString }))
        out <- Subscriber.stream[F]("projects/gcp-playground-228403/subscriptions/test-sub")(E, subscriber)
//        commited <- Subscriber.commit[F]("projects/gcp-playground-228403/subscriptions/test-sub")(out.map(_.ackId).toArray:_*)(E, subscriber)
      } yield out

      def printMsg(msg: Option[PubsubMessage]) =  msg match {
        case Some(m) =>
          val msgText = m.data.asScala.map(_.toChar).mkString
          IO.pure(println(msgText))
        case None => IO(println("Nothing found yo"))
      }

      run[IO]
        .mapAsync(1)(r => printMsg(r.message))
        .compile.toList.attempt.unsafeRunSync() match {
        case Right(v) => println(v)
        case Left(value) => throw value
      }
    }
  }
}
