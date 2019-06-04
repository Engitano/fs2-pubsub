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

import cats.syntax.either._
import cats.effect.Sync

import scala.util.Try

object Deserializer extends LowPriorityDeserializerImplicits {
  def apply[T](implicit d: Deserializer[T]): Deserializer[T] = d

  def from[T](f: Array[Byte] => T): Deserializer[T] =
    new Deserializer[T] {
      override def deserialize(b: Option[Array[Byte]]): Either[SerializationException, T] = b match {
        case Some(b) => Try(f(b)).toEither.leftMap(t => SerializationFailedException(t.getMessage))
        case None    => Left(EmptyPubsubMessageException)
      }
    }
}

trait Deserializer[T] {
  def deserialize(b: Option[Array[Byte]]): Either[SerializationException, T]
}

trait LowPriorityDeserializerImplicits {
  implicit def forByteArrayDeserializer: Deserializer[Array[Byte]] =
    new Deserializer[Array[Byte]] {
      override def deserialize(b: Option[Array[Byte]]): Either[SerializationException, Array[Byte]] = b match {
        case Some(b) => Right(b)
        case None    => Left(EmptyPubsubMessageException)
      }
    }
}
