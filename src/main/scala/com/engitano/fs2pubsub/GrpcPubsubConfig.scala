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
import com.google.auth.Credentials
import com.google.auth.oauth2.GoogleCredentials
import io.grpc.auth.MoreCallCredentials
import io.grpc.{CallCredentials, CallOptions, ManagedChannelBuilder}

object GrpcPubsubConfig {

  private val apiEndpoint = "pubsub.googleapis.com"

  def local(project: String, port: Int): GrpcPubsubConfig =
    new GrpcPubsubConfig(project, s"localhost:$port", None, true)

  def apply(project: String): GrpcPubsubConfig =
    new GrpcPubsubConfig(project, apiEndpoint, Some(MoreCallCredentials.from(GoogleCredentials.getApplicationDefault)))

  def apply(project: String, credentials: Credentials): GrpcPubsubConfig =
    new GrpcPubsubConfig(project, apiEndpoint, Some(MoreCallCredentials.from(credentials)))

  def apply(project: String, credentials: CallCredentials): GrpcPubsubConfig =
    new GrpcPubsubConfig(project, apiEndpoint, Some(credentials))
}

case class GrpcPubsubConfig(project: String, host: String, credentials: Option[CallCredentials], plainText: Boolean = false) {

  def projectPath: String                            = s"projects/$project"
  def topicName(topic: String): String               = s"$projectPath/topics/$topic"
  def subscriptionName(subscription: String): String = s"$projectPath/subscriptions/$subscription"
  def snapshotName(snapshot: String): String         = s"$projectPath/snapshots/${snapshot}"

  private[fs2pubsub] def callOps = credentials.foldLeft(CallOptions.DEFAULT)(_ withCallCredentials _)
  private[fs2pubsub] def channelBuilder: ManagedChannelBuilder[_] = {
    val channel = ManagedChannelBuilder.forTarget(host)
    if (plainText) {
      channel.usePlaintext()
    } else {
      channel
    }
  }
}
