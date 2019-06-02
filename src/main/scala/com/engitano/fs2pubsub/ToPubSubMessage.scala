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

import cats.syntax.functor._
import cats.Functor
import cats.effect.Sync
import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage

trait ToPubSubMessage[F[_], T] {
  def to(t: T): F[PubsubMessage]
}

object ToPubSubMessage extends LowPriorityToPubSubMessageImplicits {
  def apply[F[_], T](implicit psm: ToPubSubMessage[F, T]): ToPubSubMessage[F, T] = psm

}

trait LowPriorityToPubSubMessageImplicits {

  implicit def fromPubSubMessage[F[_]](implicit A: Sync[F]): ToPubSubMessage[F, PubsubMessage] =
    new ToPubSubMessage[F, PubsubMessage] {
      override def to(t: PubsubMessage): F[PubsubMessage] = A.delay(t)
    }

  implicit def fromSerializerFromPubSubMessage[F[_]: Functor, T](implicit ser: Serializer[F, T]): ToPubSubMessage[F, T] =
    new ToPubSubMessage[F, T] {
      override def to(t: T): F[PubsubMessage] = ser.serialize(t).map(m => PubsubMessage(ByteString.copyFrom(m)))
    }
}
