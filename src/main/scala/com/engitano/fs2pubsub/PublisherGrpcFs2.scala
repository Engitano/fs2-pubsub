package com.engitano.fs2pubsub

import _root_.cats.implicits._

private [fs2pubsub]trait PublisherFs2Grpc[F[_]] {
  def createTopic(request: com.google.api.pubsub.Topic, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.Topic]
  def updateTopic(request: com.google.api.pubsub.UpdateTopicRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.Topic]
  def publish(request: com.google.api.pubsub.PublishRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.PublishResponse]
  def getTopic(request: com.google.api.pubsub.GetTopicRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.Topic]
  def listTopics(request: com.google.api.pubsub.ListTopicsRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.ListTopicsResponse]
  def listTopicSubscriptions(request: com.google.api.pubsub.ListTopicSubscriptionsRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.ListTopicSubscriptionsResponse]
  def listTopicSnapshots(request: com.google.api.pubsub.ListTopicSnapshotsRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.ListTopicSnapshotsResponse]
  def deleteTopic(request: com.google.api.pubsub.DeleteTopicRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.protobuf.empty.Empty]
}
private [fs2pubsub]object PublisherFs2Grpc {
  def stub[F[_]: _root_.cats.effect.ConcurrentEffect](channel: _root_.io.grpc.Channel, callOptions: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT): PublisherFs2Grpc[F] = new PublisherFs2Grpc[F] {
    def createTopic(request: com.google.api.pubsub.Topic, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.Topic] = {
      _root_.org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall[F](channel, _root_.com.google.api.pubsub.PublisherGrpc.METHOD_CREATE_TOPIC, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
    }
    def updateTopic(request: com.google.api.pubsub.UpdateTopicRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.Topic] = {
      _root_.org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall[F](channel, _root_.com.google.api.pubsub.PublisherGrpc.METHOD_UPDATE_TOPIC, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
    }
    def publish(request: com.google.api.pubsub.PublishRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.PublishResponse] = {
      _root_.org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall[F](channel, _root_.com.google.api.pubsub.PublisherGrpc.METHOD_PUBLISH, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
    }
    def getTopic(request: com.google.api.pubsub.GetTopicRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.Topic] = {
      _root_.org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall[F](channel, _root_.com.google.api.pubsub.PublisherGrpc.METHOD_GET_TOPIC, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
    }
    def listTopics(request: com.google.api.pubsub.ListTopicsRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.ListTopicsResponse] = {
      _root_.org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall[F](channel, _root_.com.google.api.pubsub.PublisherGrpc.METHOD_LIST_TOPICS, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
    }
    def listTopicSubscriptions(request: com.google.api.pubsub.ListTopicSubscriptionsRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.ListTopicSubscriptionsResponse] = {
      _root_.org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall[F](channel, _root_.com.google.api.pubsub.PublisherGrpc.METHOD_LIST_TOPIC_SUBSCRIPTIONS, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
    }
    def listTopicSnapshots(request: com.google.api.pubsub.ListTopicSnapshotsRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.api.pubsub.ListTopicSnapshotsResponse] = {
      _root_.org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall[F](channel, _root_.com.google.api.pubsub.PublisherGrpc.METHOD_LIST_TOPIC_SNAPSHOTS, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
    }
    def deleteTopic(request: com.google.api.pubsub.DeleteTopicRequest, clientHeaders: _root_.io.grpc.Metadata): F[com.google.protobuf.empty.Empty] = {
      _root_.org.lyranthe.fs2_grpc.java_runtime.client.Fs2ClientCall[F](channel, _root_.com.google.api.pubsub.PublisherGrpc.METHOD_DELETE_TOPIC, callOptions).flatMap(_.unaryToUnaryCall(request, clientHeaders))
    }
  }
}