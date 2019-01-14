package com.engitano.fs2pubsub

import _root_.cats.implicits._
import cats.effect.ConcurrentEffect
import fs2.Stream
import io.grpc.Metadata
import org.lyranthe.fs2_grpc.java_runtime.client.Fs2StreamClientCallListener

trait SubscriberFs2Grpc[F[_]] {
  def createSubscription(request: com.google.api.pubsub.Subscription, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.Subscription]
  def getSubscription(request: com.google.api.pubsub.GetSubscriptionRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.Subscription]
  def updateSubscription(request: com.google.api.pubsub.UpdateSubscriptionRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.Subscription]
  def listSubscriptions(request: com.google.api.pubsub.ListSubscriptionsRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.ListSubscriptionsResponse]
  def deleteSubscription(request: com.google.api.pubsub.DeleteSubscriptionRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.protobuf.empty.Empty]
  def modifyAckDeadline(request: com.google.api.pubsub.ModifyAckDeadlineRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.protobuf.empty.Empty]
  def acknowledge(request: com.google.api.pubsub.AcknowledgeRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.protobuf.empty.Empty]
  def pull(request: com.google.api.pubsub.PullRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.PullResponse]
  def streamingPull(request: _root_.fs2.Stream[F, com.google.api.pubsub.StreamingPullRequest], clientHeaders: _root_.io.grpc.Metadata): _root_.fs2.Stream[F, com.google.api.pubsub.StreamingPullResponse]
  def modifyPushConfig(request: com.google.api.pubsub.ModifyPushConfigRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.protobuf.empty.Empty]
  def getSnapshot(request: com.google.api.pubsub.GetSnapshotRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.Snapshot]
  def listSnapshots(request: com.google.api.pubsub.ListSnapshotsRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.ListSnapshotsResponse]
  def createSnapshot(request: com.google.api.pubsub.CreateSnapshotRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.Snapshot]
  def updateSnapshot(request: com.google.api.pubsub.UpdateSnapshotRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.Snapshot]
  def deleteSnapshot(request: com.google.api.pubsub.DeleteSnapshotRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.protobuf.empty.Empty]
  def seek(request: com.google.api.pubsub.SeekRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.SeekResponse]
}
object SubscriberFs2Grpc {
  def stub[F[_]: _root_.cats.effect.ConcurrentEffect](channel: _root_.io.grpc.Channel, callOptions: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT)(implicit ec: _root_.scala.concurrent.ExecutionContext): SubscriberFs2Grpc[F] = new SubscriberFs2Grpc[F] {
    def createSubscription(request: com.google.api.pubsub.Subscription, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.Subscription] = {
      _root_.org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall[F](channel, _root_.com.google.api.pubsub.SubscriberGrpc.METHOD_CREATE_SUBSCRIPTION, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
    }
    def getSubscription(request: com.google.api.pubsub.GetSubscriptionRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.Subscription] = {
      _root_.org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall[F](channel, _root_.com.google.api.pubsub.SubscriberGrpc.METHOD_GET_SUBSCRIPTION, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
    }
    def updateSubscription(request: com.google.api.pubsub.UpdateSubscriptionRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.Subscription] = {
      _root_.org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall[F](channel, _root_.com.google.api.pubsub.SubscriberGrpc.METHOD_UPDATE_SUBSCRIPTION, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
    }
    def listSubscriptions(request: com.google.api.pubsub.ListSubscriptionsRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.ListSubscriptionsResponse] = {
      _root_.org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall[F](channel, _root_.com.google.api.pubsub.SubscriberGrpc.METHOD_LIST_SUBSCRIPTIONS, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
    }
    def deleteSubscription(request: com.google.api.pubsub.DeleteSubscriptionRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.protobuf.empty.Empty] = {
      _root_.org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall[F](channel, _root_.com.google.api.pubsub.SubscriberGrpc.METHOD_DELETE_SUBSCRIPTION, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
    }
    def modifyAckDeadline(request: com.google.api.pubsub.ModifyAckDeadlineRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.protobuf.empty.Empty] = {
      _root_.org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall[F](channel, _root_.com.google.api.pubsub.SubscriberGrpc.METHOD_MODIFY_ACK_DEADLINE, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
    }
    def acknowledge(request: com.google.api.pubsub.AcknowledgeRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.protobuf.empty.Empty] = {
      _root_.org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall[F](channel, _root_.com.google.api.pubsub.SubscriberGrpc.METHOD_ACKNOWLEDGE, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
    }
    def pull(request: com.google.api.pubsub.PullRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.PullResponse] = {
      _root_.org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall[F](channel, _root_.com.google.api.pubsub.SubscriberGrpc.METHOD_PULL, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
    }
    def streamingPull(request: _root_.fs2.Stream[F, com.google.api.pubsub.StreamingPullRequest], clientHeaders: _root_.io.grpc.Metadata): _root_.fs2.Stream[F, com.google.api.pubsub.StreamingPullResponse] = {
      _root_.fs2.Stream.eval(_root_.org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall[F](channel, _root_.com.google.api.pubsub.SubscriberGrpc.METHOD_STREAMING_PULL, callOptions)).flatMap(_.streamingToStreamingCall(request, clientHeaders))
    }
    def modifyPushConfig(request: com.google.api.pubsub.ModifyPushConfigRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.protobuf.empty.Empty] = {
      _root_.org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall[F](channel, _root_.com.google.api.pubsub.SubscriberGrpc.METHOD_MODIFY_PUSH_CONFIG, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
    }
    def getSnapshot(request: com.google.api.pubsub.GetSnapshotRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.Snapshot] = {
      _root_.org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall[F](channel, _root_.com.google.api.pubsub.SubscriberGrpc.METHOD_GET_SNAPSHOT, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
    }
    def listSnapshots(request: com.google.api.pubsub.ListSnapshotsRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.ListSnapshotsResponse] = {
      _root_.org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall[F](channel, _root_.com.google.api.pubsub.SubscriberGrpc.METHOD_LIST_SNAPSHOTS, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
    }
    def createSnapshot(request: com.google.api.pubsub.CreateSnapshotRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.Snapshot] = {
      _root_.org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall[F](channel, _root_.com.google.api.pubsub.SubscriberGrpc.METHOD_CREATE_SNAPSHOT, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
    }
    def updateSnapshot(request: com.google.api.pubsub.UpdateSnapshotRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.Snapshot] = {
      _root_.org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall[F](channel, _root_.com.google.api.pubsub.SubscriberGrpc.METHOD_UPDATE_SNAPSHOT, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
    }
    def deleteSnapshot(request: com.google.api.pubsub.DeleteSnapshotRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.protobuf.empty.Empty] = {
      _root_.org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall[F](channel, _root_.com.google.api.pubsub.SubscriberGrpc.METHOD_DELETE_SNAPSHOT, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
    }
    def seek(request: com.google.api.pubsub.SeekRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.SeekResponse] = {
      _root_.org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall[F](channel, _root_.com.google.api.pubsub.SubscriberGrpc.METHOD_SEEK, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
    }
  }
  def bindService[F[_]: _root_.cats.effect.ConcurrentEffect](serviceImpl: SubscriberFs2Grpc[F])(implicit ec: _root_.scala.concurrent.ExecutionContext): _root_.io.grpc.ServerServiceDefinition = {
    _root_.io.grpc.ServerServiceDefinition
      .builder(_root_.com.google.api.pubsub.SubscriberGrpc.SERVICE)
      .addMethod(_root_.com.google.api.pubsub.SubscriberGrpc.METHOD_CREATE_SUBSCRIPTION, _root_.org.lyranthe.fs2_grpc.java_runtime.server.Fs2ServerCallHandler[F].unaryToUnaryCall(serviceImpl.createSubscription))
      .addMethod(_root_.com.google.api.pubsub.SubscriberGrpc.METHOD_GET_SUBSCRIPTION, _root_.org.lyranthe.fs2_grpc.java_runtime.server.Fs2ServerCallHandler[F].unaryToUnaryCall(serviceImpl.getSubscription))
      .addMethod(_root_.com.google.api.pubsub.SubscriberGrpc.METHOD_UPDATE_SUBSCRIPTION, _root_.org.lyranthe.fs2_grpc.java_runtime.server.Fs2ServerCallHandler[F].unaryToUnaryCall(serviceImpl.updateSubscription))
      .addMethod(_root_.com.google.api.pubsub.SubscriberGrpc.METHOD_LIST_SUBSCRIPTIONS, _root_.org.lyranthe.fs2_grpc.java_runtime.server.Fs2ServerCallHandler[F].unaryToUnaryCall(serviceImpl.listSubscriptions))
      .addMethod(_root_.com.google.api.pubsub.SubscriberGrpc.METHOD_DELETE_SUBSCRIPTION, _root_.org.lyranthe.fs2_grpc.java_runtime.server.Fs2ServerCallHandler[F].unaryToUnaryCall(serviceImpl.deleteSubscription))
      .addMethod(_root_.com.google.api.pubsub.SubscriberGrpc.METHOD_MODIFY_ACK_DEADLINE, _root_.org.lyranthe.fs2_grpc.java_runtime.server.Fs2ServerCallHandler[F].unaryToUnaryCall(serviceImpl.modifyAckDeadline))
      .addMethod(_root_.com.google.api.pubsub.SubscriberGrpc.METHOD_ACKNOWLEDGE, _root_.org.lyranthe.fs2_grpc.java_runtime.server.Fs2ServerCallHandler[F].unaryToUnaryCall(serviceImpl.acknowledge))
      .addMethod(_root_.com.google.api.pubsub.SubscriberGrpc.METHOD_PULL, _root_.org.lyranthe.fs2_grpc.java_runtime.server.Fs2ServerCallHandler[F].unaryToUnaryCall(serviceImpl.pull))
      .addMethod(_root_.com.google.api.pubsub.SubscriberGrpc.METHOD_STREAMING_PULL, _root_.org.lyranthe.fs2_grpc.java_runtime.server.Fs2ServerCallHandler[F].streamingToStreamingCall(serviceImpl.streamingPull))
      .addMethod(_root_.com.google.api.pubsub.SubscriberGrpc.METHOD_MODIFY_PUSH_CONFIG, _root_.org.lyranthe.fs2_grpc.java_runtime.server.Fs2ServerCallHandler[F].unaryToUnaryCall(serviceImpl.modifyPushConfig))
      .addMethod(_root_.com.google.api.pubsub.SubscriberGrpc.METHOD_GET_SNAPSHOT, _root_.org.lyranthe.fs2_grpc.java_runtime.server.Fs2ServerCallHandler[F].unaryToUnaryCall(serviceImpl.getSnapshot))
      .addMethod(_root_.com.google.api.pubsub.SubscriberGrpc.METHOD_LIST_SNAPSHOTS, _root_.org.lyranthe.fs2_grpc.java_runtime.server.Fs2ServerCallHandler[F].unaryToUnaryCall(serviceImpl.listSnapshots))
      .addMethod(_root_.com.google.api.pubsub.SubscriberGrpc.METHOD_CREATE_SNAPSHOT, _root_.org.lyranthe.fs2_grpc.java_runtime.server.Fs2ServerCallHandler[F].unaryToUnaryCall(serviceImpl.createSnapshot))
      .addMethod(_root_.com.google.api.pubsub.SubscriberGrpc.METHOD_UPDATE_SNAPSHOT, _root_.org.lyranthe.fs2_grpc.java_runtime.server.Fs2ServerCallHandler[F].unaryToUnaryCall(serviceImpl.updateSnapshot))
      .addMethod(_root_.com.google.api.pubsub.SubscriberGrpc.METHOD_DELETE_SNAPSHOT, _root_.org.lyranthe.fs2_grpc.java_runtime.server.Fs2ServerCallHandler[F].unaryToUnaryCall(serviceImpl.deleteSnapshot))
      .addMethod(_root_.com.google.api.pubsub.SubscriberGrpc.METHOD_SEEK, _root_.org.lyranthe.fs2_grpc.java_runtime.server.Fs2ServerCallHandler[F].unaryToUnaryCall(serviceImpl.seek))
      .build()
  }
}