/*
 * Copyright (c) 2019 Engitano
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.engitano.fs2pubsub

import cats.effect._
import cats.implicits._
import com.spotify.docker.client.{DefaultDockerClient, DockerClient}
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.{DockerContainer, DockerFactory, DockerKit, DockerReadyChecker}
import fs2._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.concurrent.ExecutionContext

class E2eSpec extends WordSpec with Matchers with DockerPubSubService with BeforeAndAfterAll {

  import HasAckId._
  import com.engitano.fs2pubsub.syntax._

  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  override def beforeAll(): Unit = {
    startAllOrFail()
  }

  override def afterAll(): Unit = {
    stopAllQuietly()
  }

  "The Generated clients" should {
    "be able to read and write to PubSub" in {

      implicit val intSerializer   = Serializer.from[Int](i => BigInt(i).toByteArray)
      implicit val intDeserializer = Deserializer.from[Int](b => BigInt(b).toInt)

      val cfg = GrpcPubsubConfig.local(DefaultGcpProject, DefaultPubsubPort)

      val topicName        = "test-topic"
      val testSubscription = "test-sub"
      val msgCount         = 2000

      def businessLogic(i: Int)              = IO.unit
      def deadLetter(p: PubSubResponse[Int]) = IO.unit

      val program = (Publisher.resource[IO](cfg), Subscriber.resource[IO](cfg)).tupled.use { pubsub =>
        implicit val (pub, sub) = pubsub

        val setup =
          pub.createTopic(topicName) *>
            sub.createSubscription(testSubscription, topicName)

        val publish =
          Stream
            .emits[IO, Int](1 to msgCount)
            .toPubSub(topicName)

        val subscribe =
          sub.consume[Int](testSubscription) { s =>
            s.evalTap(
                msg =>
                  msg.body match {
                    case Right(i) => businessLogic(i)
                    case _        => deadLetter(msg)
                  }
              )
          }

        setup *> subscribe
          .concurrently(publish)
          .take(msgCount)
          .compile
          .toList
          .nested
          .map(_.body)
          .value
      }

      program.unsafeRunSync().map(_.right.get) shouldBe (1 to msgCount).toList
    }
  }
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
