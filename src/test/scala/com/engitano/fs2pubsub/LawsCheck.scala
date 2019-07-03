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

import cats.Eq
import cats.tests.CatsSuite
import org.scalacheck.ScalacheckShapeless._
import cats.laws.discipline.TraverseTests
import com.google.protobuf.ByteString
import org.scalacheck.{Arbitrary, Gen}

class LawsCheck extends CatsSuite {

  import instances.pubsubresponse._

  implicit def eqTree[A: Eq]: Eq[PubSubResponse[A]] = Eq.fromUniversalEquals

  implicit val arbitraryTimestamp: Arbitrary[ByteString] = Arbitrary[ByteString] { Gen.const(ByteString.EMPTY) }

  checkAll("PubSubResponse.FunctorLaws", TraverseTests[PubSubResponse].functor[Int, Int, String])
  checkAll("PubSubResponse.FoldableLaws", TraverseTests[PubSubResponse].foldable[Int, Int])
  checkAll("PubSubResponse.TraversableLaws", TraverseTests[PubSubResponse].traverse[Int, Int, Int, Int, Option, Option])
}
