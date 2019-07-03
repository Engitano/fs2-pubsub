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
import cats.{Applicative, Eval, Traverse}
import com.google.pubsub.v1.ReceivedMessage

case class PubSubResponse[A](ackId: String, body: Either[InvalidPubSubResponse, A])
case class InvalidPubSubResponse(ex: SerializationException, response: ReceivedMessage)

trait PubSubResponseInstances {
  implicit val catsDataTraverseForPubSubResponse: Traverse[PubSubResponse] = new Traverse[PubSubResponse] {
    override def traverse[G[_], A, B](fa: PubSubResponse[A])(f: A => G[B])(implicit G: Applicative[G]): G[PubSubResponse[B]] =
      fa.body match {
        case Left(t)  => G.pure(PubSubResponse[B](fa.ackId, Left(t)))
        case Right(a) => f(a).map(b => PubSubResponse[B](fa.ackId, Right(b)))
      }

    override def foldLeft[A, B](fa: PubSubResponse[A], b: B)(f: (B, A) => B): B = fa.body match {
      case Right(a) => f(b, a)
      case Left(_)  => b
    }

    override def foldRight[A, B](fa: PubSubResponse[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] = fa.body match {
      case Right(a) => f(a, lb)
      case Left(_)  => lb
    }
  }

  implicit def hasAckIdForPubSubResponse[T]: HasAckId[PubSubResponse[T]] =
    new HasAckId[PubSubResponse[T]] {
      override def getAckId(t: PubSubResponse[T]): String = t.ackId
    }
}
