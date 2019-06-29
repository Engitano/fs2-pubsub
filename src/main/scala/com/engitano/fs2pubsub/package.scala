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

package com.engitano

import fs2.Stream

package object fs2pubsub {

  private[fs2pubsub] def pagedRec[F[_], A, Req, Res, B](ctr: String => Req, call: Req => F[Res])(tk: Res => String, resp: Res => Seq[B]) = {
    def nextPage(token: String): fs2.Stream[F, B] =
      Stream
        .eval(call(ctr(token)))
        .flatMap(
          r =>
            Stream.emits(resp(r))
              ++ Option(tk(r)).filter(_.nonEmpty).fold[Stream[F, B]](Stream.empty)(nextPage)
        )
    nextPage("")
  }
}
