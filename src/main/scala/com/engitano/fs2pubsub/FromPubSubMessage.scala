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

import com.google.pubsub.v1.ReceivedMessage

trait FromPubSubMessage[T] {
  def from(rm: ReceivedMessage): Either[SerializationException, T]
}

object FromPubSubMessage extends FromPubSubMessageLowPriorityImplicits {
  def apply[T](implicit fpm: FromPubSubMessage[T]): FromPubSubMessage[T] = fpm
}

trait FromPubSubMessageLowPriorityImplicits {
  implicit def idFromPubsubMessage: FromPubSubMessage[ReceivedMessage] =
    new FromPubSubMessage[ReceivedMessage] {
      override def from(rm: ReceivedMessage) = Right(rm)
    }

  implicit def deserializerFromPubsubMessage[F[_], T](implicit ds: Deserializer[T]): FromPubSubMessage[T] =
    new FromPubSubMessage[T] {
      override def from(rm: ReceivedMessage): Either[SerializationException, T] = ds.deserialize(rm.message.map(_.data.toByteArray))
    }
}