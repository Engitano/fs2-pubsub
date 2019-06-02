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

import cats.Applicative
import cats.effect.Sync

trait Serializer[F[_], T] {
  def serialize(t: T): F[Array[Byte]]
}

trait LowPrioritySerializerImplicits {
  implicit def fromByteArraySerializer[F[_]](implicit A: Applicative[F]): Serializer[F, Array[Byte]] =
    new Serializer[F, Array[Byte]] {
      override def serialize(t: Array[Byte]): F[Array[Byte]] = A.pure(t)
    }
}

object Serializer extends LowPrioritySerializerImplicits {
  def apply[F[_], T](implicit s: Serializer[F, T]): Serializer[F, T] = s

  def from[F[_], T](f: T => Array[Byte])(implicit S: Sync[F]): Serializer[F, T] = new Serializer[F, T] {
    override def serialize(t: T): F[Array[Byte]] = S.delay(f(t))
  }

  def fromF[F[_], T](f: T => F[Array[Byte]]): Serializer[F, T] = new Serializer[F, T] {
    override def serialize(t: T): F[Array[Byte]] = f(t)
  }
}