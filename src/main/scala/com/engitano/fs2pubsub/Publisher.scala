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

import cats.implicits._
import cats.{Applicative, Functor}
import cats.implicits._
import cats.effect.{ConcurrentEffect, Resource, Sync}
import com.google.pubsub.v1._
import com.google.protobuf.ByteString
import io.grpc._
import fs2.Stream
import fs2.concurrent.Queue



trait Publisher[F[_]] {
  def stream[T](topic: String)(s: Stream[F, T])(implicit psm: ToPubSubMessage[T]): Stream[F, String]
  def publish[T: ToPubSubMessage[?]](topic: String, t: T): F[Unit]
  def createTopic(topic: String): F[Topic]
  def listTopics(): Stream[F, String]
  def listTopicSubscriptions(topic: String): Stream[F, String]
  def listTopicSnapshots(topic: String): Stream[F, String]
  def deleteTopic(topic: String): F[Unit]
}

object Publisher {

  private val MAX_PUBSUB_MESSAGE_BATCH = 1000

  //noinspection ScalaStyle
  def apply[F[_]: ConcurrentEffect](cfg: GrpcPubsubConfig): Resource[F, Publisher[F]] =
    buildStub[F, PublisherFs2Grpc[?[_], io.grpc.Metadata]](cfg)((ch, o) => PublisherFs2Grpc.stub[F](ch, o)).map { publisher =>
      new Publisher[F] {

        def stream[T](topic: String)(s: Stream[F, T])(implicit psm: ToPubSubMessage[T]): Stream[F, String] = {
          s.chunkLimit(MAX_PUBSUB_MESSAGE_BATCH)
            .map(c => c.map(psm.to))
            .evalMap(p => publisher.publish(PublishRequest(cfg.topicName(topic), p.toList), new Metadata()))
            .flatMap(pr => Stream.emits(pr.messageIds))
        }

        def publish[T](topic: String, t: T)(implicit psm: ToPubSubMessage[T]): F[Unit] =
          publisher.publish(PublishRequest(cfg.topicName(topic), List(psm.to(t))), new Metadata()).as(())


        def createTopic(topic: String): F[Topic] = {
          publisher.createTopic(Topic(cfg.topicName(topic)), new Metadata())
        }

        def listTopics(): Stream[F, String] = {
          Stream.eval(Queue.unbounded[F, Option[String]]).flatMap { queue =>
            def nextPage(token: Option[String]): F[ListTopicsResponse] =
              publisher
                .listTopics(ListTopicsRequest(cfg.project, 0, token.getOrElse("")), new Metadata())
                .flatTap(r => r.topics.toList.traverse[F, Unit](t => queue.enqueue1(t.name.split("/").lastOption)))
                .flatTap(r => nextPage(Option(r.nextPageToken).filter(_.nonEmpty)))

            Stream.eval(nextPage(None)) >> queue.dequeue.unNoneTerminate
          }
        }

        def listTopicSubscriptions(topic: String): Stream[F, String] = {
          Stream.eval(Queue.unbounded[F, Option[String]]).flatMap { queue =>
            def nextPage(token: Option[String]): F[ListTopicSubscriptionsResponse] =
              publisher
                .listTopicSubscriptions(ListTopicSubscriptionsRequest(cfg.topicName(topic), 0, token.getOrElse("")), new Metadata())
                .flatTap(r => r.subscriptions.toList.traverse[F, Unit](t => queue.enqueue1(t.split("/").lastOption)))
                .flatTap(r => nextPage(Option(r.nextPageToken).filter(_.nonEmpty)))

            Stream.eval(nextPage(None)) >> queue.dequeue.unNoneTerminate
          }
        }
        def listTopicSnapshots(topic: String): Stream[F, String] = {
          Stream.eval(Queue.unbounded[F, Option[String]]).flatMap { queue =>
            def nextPage(token: Option[String]): F[ListTopicSnapshotsResponse] =
              publisher
                .listTopicSnapshots(ListTopicSnapshotsRequest(cfg.topicName(topic), 0, token.getOrElse("")), new Metadata())
                .flatTap(r => r.snapshots.toList.traverse[F, Unit](t => queue.enqueue1(t.split("/").lastOption)))
                .flatTap(r => nextPage(Option(r.nextPageToken).filter(_.nonEmpty)))

            Stream.eval(nextPage(None)) >> queue.dequeue.unNoneTerminate
          }
        }

        def deleteTopic(topic: String): F[Unit] = {
          publisher.deleteTopic(DeleteTopicRequest(cfg.topicName(topic)), new Metadata()).as(())
        }
      }
    }
}
