package com.engitano.fs2pubsub

import com.engitano.fs2pubsub.PushMessage.MessageDef

object PushMessage {
  case class MessageDef(attributes: Map[String, String],data: String, messageId: String)
}

case class PushMessage(message: MessageDef, subscription: String)
