package com.engitano.fs2pubsub

import cats.implicits._
import cats.effect.{ConcurrentEffect, Sync, Timer}
import com.google.api.pubsub.{AcknowledgeRequest, PullRequest, ReceivedMessage, StreamingPullRequest}
import fs2.Stream
import fs2.concurrent.Queue
import io.grpc.Metadata

import scala.concurrent.duration._

object Subscriber {
  def stream[F[_]](subscription: String)(implicit S: ConcurrentEffect[F], sub: SubscriberFs2Grpc[F]): Stream[F, ReceivedMessage] = {
    Stream.eval(Queue.unbounded[F, StreamingPullRequest]) flatMap { requestQueue =>
      def queueNext(req: StreamingPullRequest): F[Unit] = {
        requestQueue.enqueue1(req).as(())
      }

      Stream.eval(queueNext(StreamingPullRequest(subscription, Seq(), Seq(), Seq(), 10))).flatMap { _ =>
        sub.streamingPull(requestQueue.dequeue, new Metadata()).flatMap { resp =>
          Stream.emits(resp.receivedMessages) ++ Stream.eval_(queueNext(StreamingPullRequest(ackIds = resp.receivedMessages.map(_.ackId))))
        }
      }
    }
  }
}
