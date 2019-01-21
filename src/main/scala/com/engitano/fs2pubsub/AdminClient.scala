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

package com.engitano.fs2pubsub

import cats.Functor
import cats.implicits._
import cats.effect.{ConcurrentEffect, Resource}
import com.google.api.pubsub._
import io.grpc._
import org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall
import org.lyranthe.fs2_grpc.java_runtime.syntax.all._

object AdminClient {
  def create[F[_]: ConcurrentEffect](cfg: GrpcPubsubConfig): Resource[F, AdminClient[F]] = {

    type MananagedChannelResourse[A] = Resource[F, A]

    val res: MananagedChannelResourse[ManagedChannel] = cfg.channelBuilder.resource[F]

    res.map(channel => new AdminClient[F](channel, cfg.callOps))
  }
}

class AdminClient[F[_]: ConcurrentEffect] private(channel: Channel, callOptions: CallOptions) {

  // Publisher related methods
  def createTopic(request: Topic, clientHeaders: Metadata): F[Topic] = {
    Fs2ClientCall[F](channel, PublisherGrpc.METHOD_CREATE_TOPIC, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
  }
  def updateTopic(request: UpdateTopicRequest, clientHeaders: io.grpc.Metadata): F[Topic] = {
    Fs2ClientCall[F](channel, PublisherGrpc.METHOD_UPDATE_TOPIC, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
  }
  def publish(request: PublishRequest, clientHeaders: io.grpc.Metadata): F[PublishResponse] = {
    Fs2ClientCall[F](channel, PublisherGrpc.METHOD_PUBLISH, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
  }
  def getTopic(request: GetTopicRequest, clientHeaders: io.grpc.Metadata): F[Topic] = {
    Fs2ClientCall[F](channel, PublisherGrpc.METHOD_GET_TOPIC, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
  }
  def listTopics(request: ListTopicsRequest, clientHeaders: io.grpc.Metadata): F[ListTopicsResponse] = {
    Fs2ClientCall[F](channel, PublisherGrpc.METHOD_LIST_TOPICS, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
  }
  def listTopicSubscriptions(request: ListTopicSubscriptionsRequest, clientHeaders: io.grpc.Metadata): F[ListTopicSubscriptionsResponse] = {
    Fs2ClientCall[F](channel, PublisherGrpc.METHOD_LIST_TOPIC_SUBSCRIPTIONS, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
  }
  def listTopicSnapshots(request: ListTopicSnapshotsRequest, clientHeaders: io.grpc.Metadata): F[ListTopicSnapshotsResponse] = {
    Fs2ClientCall[F](channel, PublisherGrpc.METHOD_LIST_TOPIC_SNAPSHOTS, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
  }
  def deleteTopic(request: DeleteTopicRequest, clientHeaders: io.grpc.Metadata): F[com.google.protobuf.empty.Empty] = {
    Fs2ClientCall[F](channel, PublisherGrpc.METHOD_DELETE_TOPIC, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
  }

  // Subscription related methods
  def createSubscription(request: Subscription, clientHeaders: io.grpc.Metadata): F[Subscription] = {
    Fs2ClientCall[F](channel, SubscriberGrpc.METHOD_CREATE_SUBSCRIPTION, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
  }
  def getSubscription(request: GetSubscriptionRequest, clientHeaders: io.grpc.Metadata): F[Subscription] = {
    Fs2ClientCall[F](channel, SubscriberGrpc.METHOD_GET_SUBSCRIPTION, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
  }
  def updateSubscription(request: UpdateSubscriptionRequest, clientHeaders: io.grpc.Metadata): F[Subscription] = {
    Fs2ClientCall[F](channel, SubscriberGrpc.METHOD_UPDATE_SUBSCRIPTION, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
  }
  def listSubscriptions(request: ListSubscriptionsRequest, clientHeaders: io.grpc.Metadata): F[ListSubscriptionsResponse] = {
    Fs2ClientCall[F](channel, SubscriberGrpc.METHOD_LIST_SUBSCRIPTIONS, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
  }
  def deleteSubscription(request: DeleteSubscriptionRequest, clientHeaders: io.grpc.Metadata): F[com.google.protobuf.empty.Empty] = {
    Fs2ClientCall[F](channel, SubscriberGrpc.METHOD_DELETE_SUBSCRIPTION, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
  }
  def modifyAckDeadline(request: ModifyAckDeadlineRequest, clientHeaders: io.grpc.Metadata): F[com.google.protobuf.empty.Empty] = {
    Fs2ClientCall[F](channel, SubscriberGrpc.METHOD_MODIFY_ACK_DEADLINE, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
  }
  def acknowledge(request: AcknowledgeRequest, clientHeaders: io.grpc.Metadata): F[com.google.protobuf.empty.Empty] = {
    Fs2ClientCall[F](channel, SubscriberGrpc.METHOD_ACKNOWLEDGE, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
  }
  def pull(request: PullRequest, clientHeaders: io.grpc.Metadata): F[PullResponse] = {
    Fs2ClientCall[F](channel, SubscriberGrpc.METHOD_PULL, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
  }
  def modifyPushConfig(request: ModifyPushConfigRequest, clientHeaders: io.grpc.Metadata): F[com.google.protobuf.empty.Empty] = {
    Fs2ClientCall[F](channel, SubscriberGrpc.METHOD_MODIFY_PUSH_CONFIG, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
  }
  def getSnapshot(request: GetSnapshotRequest, clientHeaders: io.grpc.Metadata): F[Snapshot] = {
    Fs2ClientCall[F](channel, SubscriberGrpc.METHOD_GET_SNAPSHOT, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
  }
  def listSnapshots(request: ListSnapshotsRequest, clientHeaders: io.grpc.Metadata): F[ListSnapshotsResponse] = {
    Fs2ClientCall[F](channel, SubscriberGrpc.METHOD_LIST_SNAPSHOTS, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
  }
  def createSnapshot(request: CreateSnapshotRequest, clientHeaders: io.grpc.Metadata): F[Snapshot] = {
    Fs2ClientCall[F](channel, SubscriberGrpc.METHOD_CREATE_SNAPSHOT, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
  }
  def updateSnapshot(request: UpdateSnapshotRequest, clientHeaders: io.grpc.Metadata): F[Snapshot] = {
    Fs2ClientCall[F](channel, SubscriberGrpc.METHOD_UPDATE_SNAPSHOT, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
  }
  def deleteSnapshot(request: DeleteSnapshotRequest, clientHeaders: io.grpc.Metadata): F[com.google.protobuf.empty.Empty] = {
    Fs2ClientCall[F](channel, SubscriberGrpc.METHOD_DELETE_SNAPSHOT, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
  }
}
