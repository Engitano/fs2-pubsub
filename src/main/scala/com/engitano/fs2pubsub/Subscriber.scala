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

import cats.effect.{ConcurrentEffect, Resource, Sync}
import cats.implicits._
import com.engitano.fs2pubsub.Subscriber.SubscriptionConsumer
import com.google.protobuf.duration.Duration
import com.google.pubsub.v1
import com.google.pubsub.v1._
import fs2.concurrent.Queue
import fs2.{Pipe, Stream}
import io.grpc._
import org.lyranthe.fs2_grpc.java_runtime.syntax.all._


trait Subscriber[F[_]] {
  def consume[A](subscription: String): SubscriptionConsumer[F, A]
  def createSubscription(
                          name: String,
                          topic: String,
                          ackDeadlineSecs: Option[Int] = None,
                          retainAckedMsgs: Boolean = false,
                          messageRetentionDurationSeconds: Option[Int] = None,
                          withMsgOrdering: Boolean = false,
                          msgTtl: Option[Int] = None
                        ): F[Unit]

  def getSubscription(name: String): F[com.engitano.fs2pubsub.Subscription]

  def updateSubscription(
                          name: String,
                          topic: String,
                          ackDeadlineSecs: Option[Int],
                          retainAckedMsgs: Boolean = false,
                          messageRetentionDurationSeconds: Option[Int] = None,
                          withMsgOrdering: Boolean = false,
                          msgTtl: Option[Int] = None
                        ): F[Unit]

  def listSubscriptions(): Stream[F, String]

  def deleteSubscription(subscription: String): F[Unit]

  def modifyAckDeadline(subscription: String, msgIds: Seq[String], ackDeadlineSeconds: Int): F[Unit]

  def acknowledge(subscription: String, ackIds: Seq[String]): F[Unit]

  def pull[A: FromPubSubMessage](
                                  subscription: String,
                                  returnImmediately: Boolean = false,
                                  maxMessages: Option[Int] = None
                                ): F[Seq[Either[SerializationException, A]]]

  def getSnapshot(snapshot: String): F[Snapshot]

  def listSnapshots(): Stream[F, Snapshot]

  def createSnapshot(subscription: String, name: Option[String]): F[Snapshot]

  def deleteSnapshot(snapshot: String): F[Unit]

  def client: SubscriberFs2Grpc[F, Metadata]
}

case class PullResponse[A](a: A, ackId: String)

case class Snapshot(
    name: String,
    topic: String,
    expireTime: Option[com.google.protobuf.timestamp.Timestamp]
)

case class Subscription(
                         name: String,
                         topic: String,
                         ackDeadlineSeconds: Int,
                         retainAckedMessages: Boolean,
                         messageRetentionDuration: Option[scala.concurrent.duration.Duration],
                         enableMessageOrdering: Boolean,
                         messageTtl: Option[scala.concurrent.duration.Duration]
                       )

object Subscriber {
  import HasAckId._

  private val ACK_DEADLINE_SECONDS = 10

  class SubscriptionConsumer[F[_]: ConcurrentEffect, A](subscription: String, subscriber: SubscriberFs2Grpc[F, Metadata]) {
    def apply[B: HasAckId](handler: Pipe[F, PubSubResponse[A], B])(implicit fps: FromPubSubMessage[A]): Stream[F, B] = {

      Stream.eval(Queue.unbounded[F, StreamingPullRequest]).flatMap { requestQueue =>
        def queueNext(req: StreamingPullRequest): F[Unit] = {
          requestQueue.enqueue1(req).as(())
        }

        val kickOff = Stream.eval(queueNext(StreamingPullRequest(subscription, Seq(), Seq(), Seq(), ACK_DEADLINE_SECONDS)))
        val doPull = subscriber
          .streamingPull(requestQueue.dequeue, new Metadata())
          .map(_.receivedMessages.map(r => PubSubResponse(r.ackId, fps.from(r).leftMap(ex => InvalidPubSubResponse(ex, r)))))

        for {
          chunks        <- kickOff >> doPull
          emit          <- chunks.foldLeft[Stream[F, PubSubResponse[A]]](Stream.empty)((s, n) => s ++ Stream.emit(n)).through(handler)
          emitAndCommit <- Stream.emit(emit) ++ Stream.eval_(queueNext(StreamingPullRequest(ackIds = Seq(emit.ackId))))
        } yield emitAndCommit
      }
    }
  }

  def stream[F[_]: ConcurrentEffect](cfg: GrpcPubsubConfig): fs2.Stream[F,Subscriber[F]] =
    cfg.channelBuilder.stream[F].map(c => apply(cfg, c))

  def resource[F[_]: ConcurrentEffect](cfg: GrpcPubsubConfig): Resource[F,Subscriber[F]] =
    cfg.channelBuilder.resource[F].map(c => apply(cfg, c))

  def apply[F[_]: ConcurrentEffect](cfg: GrpcPubsubConfig): F[Subscriber[F]] =
    Sync[F].delay(cfg.channelBuilder.build()).map(c => apply(cfg, SubscriberFs2Grpc.stub[F](c, cfg.callOps)))

  def unsafe[F[_]: ConcurrentEffect](cfg: GrpcPubsubConfig): Subscriber[F] =
    apply(cfg, SubscriberFs2Grpc.stub[F](cfg.channelBuilder.build(), cfg.callOps))

  def apply[F[_]: ConcurrentEffect](cfg: GrpcPubsubConfig, channel: Channel): Subscriber[F] =
    apply(cfg, SubscriberFs2Grpc.stub[F](channel, cfg.callOps))

  //noinspection ScalaStyle
  def apply[F[_]: ConcurrentEffect](cfg: GrpcPubsubConfig, subscriber: SubscriberFs2Grpc[F, Metadata]): Subscriber[F] =
      new Subscriber[F] {
        override def consume[A](subscription: String): SubscriptionConsumer[F, A] = {
          new SubscriptionConsumer(cfg.subscriptionName(subscription), subscriber)
        }

        override def createSubscription(
                                         name: String,
                                         topic: String,
                                         ackDeadlineSecs: Option[Int] = None,
                                         retainAckedMsgs: Boolean = false,
                                         messageRetentionDurationSeconds: Option[Int] = None,
                                         withMsgOrdering: Boolean = false,
                                         msgTtl: Option[Int] = None
                                       ): F[Unit] = {
          subscriber
            .createSubscription(
              v1.Subscription(
                cfg.subscriptionName(name),
                cfg.topicName(topic),
                ackDeadlineSeconds = ackDeadlineSecs.getOrElse(0),
                retainAckedMessages = retainAckedMsgs,
                messageRetentionDuration = messageRetentionDurationSeconds.map(d => Duration(d)),
                enableMessageOrdering = withMsgOrdering,
                expirationPolicy = msgTtl.map(d => ExpirationPolicy(Some(Duration(d))))
              ),
              new Metadata()
            )
            .as(())
        }

        def getSubscription(name: String): F[com.engitano.fs2pubsub.Subscription] =
          subscriber
            .getSubscription(GetSubscriptionRequest(cfg.subscriptionName(name)), new Metadata())
            .map(s => com.engitano.fs2pubsub.Subscription(
              s.name,
              s.topic,
              s.ackDeadlineSeconds,
              s.retainAckedMessages,
              s.messageRetentionDuration.map(d => scala.concurrent.duration.Duration.fromNanos(d.nanos)),
              s.enableMessageOrdering,
              s.expirationPolicy.flatMap(_.ttl).map(d => scala.concurrent.duration.Duration.fromNanos(d.nanos))
            ))

        def updateSubscription(
                                name: String,
                                topic: String,
                                ackDeadlineSecs: Option[Int],
                                retainAckedMsgs: Boolean = false,
                                messageRetentionDurationSeconds: Option[Int] = None,
                                withMsgOrdering: Boolean = false,
                                msgTtl: Option[Int] = None
                              ): F[Unit] = {
          subscriber
            .updateSubscription(
              UpdateSubscriptionRequest(
                Some(
                  v1.Subscription(
                    cfg.subscriptionName(name),
                    cfg.topicName(topic),
                    ackDeadlineSeconds = ackDeadlineSecs.getOrElse(0),
                    retainAckedMessages = retainAckedMsgs,
                    messageRetentionDuration = messageRetentionDurationSeconds.map(d => Duration(d)),
                    enableMessageOrdering = withMsgOrdering,
                    expirationPolicy = msgTtl.map(d => ExpirationPolicy(Some(Duration(d))))
                  )
                )
              ),
              new Metadata()
            )
            .as(())
        }

        def listSubscriptions(): Stream[F, String] = {
          Stream.eval(Queue.unbounded[F, Option[String]]).flatMap { queue =>
            def nextPage(token: String): F[ListSubscriptionsResponse] =
              subscriber
                .listSubscriptions(ListSubscriptionsRequest(cfg.projectPath, 0, token), new Metadata())
                .flatTap(r => r.subscriptions.toList.map(_.name.split("/").last).toQueue(queue))
                .flatTap(r => Option(r.nextPageToken).filter(_.nonEmpty).traverse(nextPage) )

            Stream.eval(nextPage("")) >> queue.dequeue.unNoneTerminate
          }
        }

        def deleteSubscription(subscription: String) =
          subscriber
            .deleteSubscription(DeleteSubscriptionRequest(cfg.subscriptionName(subscription)), new Metadata())
            .as(())

        def modifyAckDeadline(subscription: String, msgIds: Seq[String], ackDeadlineSeconds: Int) =
          subscriber
            .modifyAckDeadline(ModifyAckDeadlineRequest(cfg.subscriptionName(subscription), msgIds, ackDeadlineSeconds), new Metadata())
            .as(())

        def acknowledge(subscription: String, ackIds: Seq[String]) =
          subscriber
            .acknowledge(AcknowledgeRequest(cfg.subscriptionName(subscription), ackIds), new Metadata())
            .as(())

        def pull[A: FromPubSubMessage](
                                        subscription: String,
                                        returnImmediately: Boolean = false,
                                        maxMessages: Option[Int] = None
                                      ): F[Seq[Either[SerializationException, A]]] =
          subscriber
            .pull(PullRequest(subscription, returnImmediately, maxMessages.getOrElse(0)), new Metadata())
            .map(r => r.receivedMessages.map(m => FromPubSubMessage[A].from(m)))

        def getSnapshot(snapshot: String): F[Snapshot] =
          subscriber.getSnapshot(GetSnapshotRequest(cfg.snapshotName(snapshot)), new Metadata())
            .map(s => com.engitano.fs2pubsub.Snapshot(s.name.split("/").last, s.topic, s.expireTime))

        def listSnapshots(): Stream[F, Snapshot] = {
          Stream.eval(Queue.unbounded[F, Option[com.engitano.fs2pubsub.Snapshot]]).flatMap { queue =>
            def nextPage(token: Option[String]): F[ListSnapshotsResponse] =
              subscriber
                .listSnapshots(ListSnapshotsRequest(cfg.projectPath, 0, token.getOrElse("")), new Metadata())
                .flatTap(
                  r =>
                    r.snapshots.toList
                      .traverse[F, Unit](t => queue.enqueue1(Some(com.engitano.fs2pubsub.Snapshot(t.name.split("/").last, t.topic, t.expireTime))))
                )
                .flatTap(r => nextPage(Option(r.nextPageToken).filter(_.nonEmpty)))

            Stream.eval(nextPage(None)) >> queue.dequeue.unNoneTerminate
          }
        }

        def createSnapshot(subscription: String, name: Option[String]): F[Snapshot] =
          subscriber
            .createSnapshot(CreateSnapshotRequest(name.map(cfg.snapshotName).getOrElse(""), cfg.snapshotName(subscription), Map()), new Metadata())
            .map(s => com.engitano.fs2pubsub.Snapshot(s.name.split("/").last, s.topic, s.expireTime))

        def deleteSnapshot(snapshot: String): F[Unit] =
          subscriber
            .deleteSnapshot(DeleteSnapshotRequest(cfg.snapshotName(snapshot)), new Metadata())
            .as(())

        def client: SubscriberFs2Grpc[F, Metadata] = subscriber

      }
}
