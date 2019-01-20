package com.engitano

import com.google.auth.Credentials
import com.google.auth.oauth2.GoogleCredentials
import io.grpc.{CallCredentials, CallOptions}
import io.grpc.auth.MoreCallCredentials

package object fs2pubsub {

  object GrpcPubsubConfig {
    def local(project: String, port: Int) : GrpcPubsubConfig = new GrpcPubsubConfig(project, s"localhost:$port", None)
    def apply(project: String): GrpcPubsubConfig = new GrpcPubsubConfig(project, "pubsub.googleapis.com", Some(MoreCallCredentials.from(GoogleCredentials.getApplicationDefault)))
    def apply(project: String, credentials: Credentials): GrpcPubsubConfig = new GrpcPubsubConfig(project, "pubsub.googleapis.com", Some(MoreCallCredentials.from(credentials)))
    def apply(project: String, credentials: CallCredentials): GrpcPubsubConfig = new GrpcPubsubConfig(project, "pubsub.googleapis.com", Some(credentials))
  }

  case class GrpcPubsubConfig(project: String, host: String, credentials: Option[CallCredentials]) {
    def topicName(topic: String) = s"projects/$project/topics/$topic"
    def subscriptionName(subscription: String) = s"projects/$project/subscriptions/$subscription"
    def callOps = credentials.foldLeft(CallOptions.DEFAULT)(_ withCallCredentials _)
  }
}
