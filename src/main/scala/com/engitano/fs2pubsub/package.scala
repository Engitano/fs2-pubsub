/*
 * Copyright 2019 Engitano
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.engitano

import com.google.auth.Credentials
import com.google.auth.oauth2.GoogleCredentials
import io.grpc.{CallCredentials, CallOptions, ManagedChannelBuilder}
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
}
