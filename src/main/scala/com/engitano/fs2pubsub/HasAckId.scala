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

trait HasAckId[T] {
  def getAckId(t: T): String
}

object HasAckId {

  def apply[T](implicit haid: HasAckId[T]): HasAckId[T] = haid

  implicit def receivedMessageHasAckId: HasAckId[ReceivedMessage] =
    new HasAckId[ReceivedMessage] {
      override def getAckId(t: ReceivedMessage): String = t.ackId
    }

  implicit def withAckIdHasAckId[T]: HasAckId[PubSubResponse[T]] =
    new HasAckId[PubSubResponse[T]] {
      override def getAckId(t: PubSubResponse[T]): String = t.ackId
    }

  implicit class HasAckIdOps[T](t: T) {
    def ackId(implicit h: HasAckId[T]): String = h.getAckId(t)
  }
}