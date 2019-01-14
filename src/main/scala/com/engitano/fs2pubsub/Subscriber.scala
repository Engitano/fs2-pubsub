package com.engitano.fs2pubsub

import cats.implicits._
import cats.effect.{ConcurrentEffect, Sync, Timer}
import com.google.api.pubsub.{AcknowledgeRequest, PullRequest, ReceivedMessage, StreamingPullRequest}
import com.google.auth.oauth2.GoogleCredentials
import fs2.Stream
import fs2.concurrent.Queue
import io.grpc.auth.MoreCallCredentials
import io.grpc.{CallOptions, ManagedChannelBuilder, Metadata}
import org.lyranthe.fs2_grpc.java_runtime.syntax.all._

import scala.concurrent.duration._

object Subscriber {
  def stream[F[_]](host: String, subscription: String)(implicit S: ConcurrentEffect[F]): Stream[F, ReceivedMessage] = {

    ManagedChannelBuilder
      .forTarget("pubsub.googleapis.com")
      .stream[F] flatMap { channelBuilder =>

      val sub = SubscriberFs2Grpc.stub(channelBuilder, CallOptions.DEFAULT.withCallCredentials(MoreCallCredentials.from(GoogleCredentials.getApplicationDefault())))
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
}
