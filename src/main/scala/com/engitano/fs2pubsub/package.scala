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

package com.engitano

import cats.implicits._
import cats.effect.{ConcurrentEffect, Resource}
import com.google.auth.Credentials
import com.google.auth.oauth2.GoogleCredentials
import com.google.pubsub.v1.{PublisherFs2Grpc, SubscriberFs2Grpc}
import fs2.Stream
import org.lyranthe.fs2_grpc.java_runtime.implicits._
import io.grpc.{CallCredentials, CallOptions, Channel, ManagedChannel, ManagedChannelBuilder}
import io.grpc.auth.MoreCallCredentials

package object fs2pubsub {

  object GrpcPubsubConfig {
    def local(project: String, port: Int) : GrpcPubsubConfig =
      new GrpcPubsubConfig(project, s"localhost:$port", None, true)

    def apply(project: String): GrpcPubsubConfig =
      new GrpcPubsubConfig(project, "pubsub.googleapis.com",
        Some(MoreCallCredentials.from(GoogleCredentials.getApplicationDefault)))

    def apply(project: String, credentials: Credentials): GrpcPubsubConfig =
      new GrpcPubsubConfig(project, "pubsub.googleapis.com", Some(MoreCallCredentials.from(credentials)))

    def apply(project: String, credentials: CallCredentials): GrpcPubsubConfig =
      new GrpcPubsubConfig(project, "pubsub.googleapis.com", Some(credentials))
  }

  case class GrpcPubsubConfig(project: String, host: String, credentials: Option[CallCredentials], plainText: Boolean = false) {

    def topicName(topic: String): String = s"projects/$project/topics/$topic"
    def subscriptionName(subscription: String): String = s"projects/$project/subscriptions/$subscription"
    def snapshotName(snapshot: String): String = s"projects/${project}/snapshots/${snapshot}"

    private[fs2pubsub] def callOps = credentials.foldLeft(CallOptions.DEFAULT)(_ withCallCredentials _)
    private[fs2pubsub] def channelBuilder = {
      val channel = ManagedChannelBuilder.forTarget(host)
      if(plainText) {
        channel.usePlaintext()
      } else {
        channel
      }
    }
  }

  object syntax {

    implicit def toOps[F[_] : ConcurrentEffect, T](s: Stream[F, T])(implicit psm: ToPubSubMessage[F, T], pub: Publisher[F]): PublisherSyntax[F, T] =
      new PublisherSyntax[F, T](s)

    class PublisherSyntax[F[_] : ConcurrentEffect, T](s: Stream[F, T])(implicit psm: ToPubSubMessage[F, T], pub: Publisher[F]) {
      def toPubSub(topic: String): Stream[F, String] = pub.stream(topic)(s)
    }
  }

  private[fs2pubsub] def buildStub[F[_]: ConcurrentEffect, A[?[_]]](cfg: GrpcPubsubConfig)(ctr: (Channel, CallOptions) => A[F]): Resource[F, A[F]] = {
    type MananagedChannelResourse[A] = Resource[F, A]

    val res: MananagedChannelResourse[ManagedChannel] = cfg.channelBuilder.resource[F]

    res.map(channel => ctr(channel, cfg.callOps))
  }

}
