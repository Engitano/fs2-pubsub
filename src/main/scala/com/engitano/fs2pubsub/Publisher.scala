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

import com.engitano.fs2pubsub._
import cats.effect.{ConcurrentEffect, Resource, Sync}
import cats.implicits._
import com.google.pubsub.v1._
import fs2.Stream
import fs2.concurrent.Queue
import io.grpc._
import org.lyranthe.fs2_grpc.java_runtime.syntax.all._

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

  def stream[F[_]: ConcurrentEffect](cfg: GrpcPubsubConfig): fs2.Stream[F, Publisher[F]] =
    cfg.channelBuilder.stream[F].map(c => apply(cfg, c))

  def resource[F[_]: ConcurrentEffect](cfg: GrpcPubsubConfig): Resource[F, Publisher[F]] =
    cfg.channelBuilder.resource[F].map(c => apply(cfg, c))

  def apply[F[_]: ConcurrentEffect](cfg: GrpcPubsubConfig): F[Publisher[F]] =
    Sync[F].delay(cfg.channelBuilder.build()).map(c => apply(cfg, PublisherFs2Grpc.stub[F](c, cfg.callOps)))

  def unsafe[F[_]: ConcurrentEffect](cfg: GrpcPubsubConfig): Publisher[F] =
    apply(cfg, PublisherFs2Grpc.stub[F](cfg.channelBuilder.build(), cfg.callOps))

  def apply[F[_]: ConcurrentEffect](cfg: GrpcPubsubConfig, channel: Channel): Publisher[F] =
    apply(cfg, PublisherFs2Grpc.stub[F](channel, cfg.callOps))

  //noinspection ScalaStyle
  def apply[F[_]: ConcurrentEffect](cfg: GrpcPubsubConfig, publisher: PublisherFs2Grpc[F, Metadata]): Publisher[F] =
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
              .flatTap(r => r.topics.toList.map(_.name.split("/").last).toQueue(queue))
              .flatTap(r => nextPage(Option(r.nextPageToken).filter(_.nonEmpty)))

          Stream.eval(nextPage(None)) >> queue.dequeue.unNoneTerminate
        }
      }

      def listTopicSubscriptions(topic: String): Stream[F, String] = {
        Stream.eval(Queue.unbounded[F, Option[String]]).flatMap { queue =>
          def nextPage(token: Option[String]): F[ListTopicSubscriptionsResponse] =
            publisher
              .listTopicSubscriptions(ListTopicSubscriptionsRequest(cfg.topicName(topic), 0, token.getOrElse("")), new Metadata())
              .flatTap(r => r.subscriptions.toList.map(_.split("/").last).toQueue(queue))
              .flatTap(r => nextPage(Option(r.nextPageToken).filter(_.nonEmpty)))

          Stream.eval(nextPage(None)) >> queue.dequeue.unNoneTerminate
        }
      }

      def listTopicSnapshots(topic: String): Stream[F, String] = {
        Stream.eval(Queue.unbounded[F, Option[String]]).flatMap { queue =>
          def nextPage(token: Option[String]): F[ListTopicSnapshotsResponse] =
            publisher
              .listTopicSnapshots(ListTopicSnapshotsRequest(cfg.topicName(topic), 0, token.getOrElse("")), new Metadata())
              .flatTap(r => r.snapshots.toList.map(_.split("/").last).toQueue(queue))
              .flatTap(r => nextPage(Option(r.nextPageToken).filter(_.nonEmpty)))

          Stream.eval(nextPage(None)) >> queue.dequeue.unNoneTerminate
        }
      }

      def deleteTopic(topic: String): F[Unit] = {
        publisher.deleteTopic(DeleteTopicRequest(cfg.topicName(topic)), new Metadata()).as(())
      }
    }

}
