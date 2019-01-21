package com.engitano.fs2pubsub

import scala.concurrent.ExecutionContext
import cats.implicits._
import cats.effect._
import com.google.api.pubsub._
import com.spotify.docker.client.{DefaultDockerClient, DockerClient}
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import fs2._
import io.grpc.Metadata
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import com.whisk.docker.{DockerContainer, DockerFactory, DockerKit, DockerReadyChecker}

class E2eSpec extends WordSpec with Matchers with DockerPubSubService with BeforeAndAfterAll {

  import HasAckId._
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  override def beforeAll(): Unit = {
    startAllOrFail()
  }

  override def afterAll(): Unit = {
    stopAllQuietly()
  }

  "The Generated clients" should {
    "be able to read and write to PubSub" in {

      val msgCount = 2000

      implicit val intSerializer = Serializer.from[IO, Int](i => BigInt(i).toByteArray)
      implicit val intDeserializer = Deserializer.from[IO, Int](b => BigInt(b).toInt)
      val cfg = GrpcPubsubConfig.local(DefaultGcpProject, DefaultPubsubPort)

      def adminTopic[F[_]: ConcurrentEffect] = AdminClient.create[F](cfg)

      def setup[F[_]: ConcurrentEffect] = adminTopic[F].use { c =>
        for {
          _ <- c.createTopic(Topic(cfg.topicName("test-topic")), new Metadata())
          _ <- c.createSubscription(Subscription(cfg.subscriptionName("test-sub"), cfg.topicName("test-topic")), new Metadata())
        } yield ()
      }

      def publisher[F[_]: ConcurrentEffect](implicit T: ToPubSubMessage[F, Int]) =
        Publisher.stream[F,Int]("test-topic", cfg)(Stream.emits(1 to msgCount))

      def subscriber[F[_]: ConcurrentEffect](implicit T: FromPubSubMessage[F, Int]) =
        Subscriber.stream[F,Int]("test-sub", cfg)(r => r)

      def run[F[_]](implicit E: ConcurrentEffect[F], T: ToPubSubMessage[F, Int], F: FromPubSubMessage[F, Int]) = for {
        _ <- Stream.eval(setup[F])
        ints <- subscriber[F].concurrently(publisher)
      } yield ints

      run[IO]
        .take(msgCount)
        .compile.toList.attempt.unsafeRunSync() match {
        case Right(v) => v.map(_.wrapped) shouldBe (1 to msgCount)
        case Left(value) => throw value
      }
    }
  }

  def identity[F[_]](sr: Stream[F, ReceivedMessage]): Stream[F, ReceivedMessage] = sr

}


trait DockerPubSubService extends DockerKit {

  val DefaultPubsubPort = 8085

  val DefaultGcpProject = "test-project"

  private val client: DockerClient = DefaultDockerClient.fromEnv().build()

  override implicit def dockerFactory: DockerFactory = new SpotifyDockerFactory(client)

  val pubsub = DockerContainer("mtranter/gcp-pubsub-emulator:latest")
    .withPorts(DefaultPubsubPort -> Some(DefaultPubsubPort))
    .withReadyChecker(DockerReadyChecker.LogLineContains("Server started"))
    .withCommand("--project", DefaultGcpProject, "--log-http", "--host-port", s"0.0.0.0:$DefaultPubsubPort")


  abstract override def dockerContainers = pubsub :: super.dockerContainers
}