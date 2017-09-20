/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package utils

import cats.data.OptionT

import scala.concurrent.Future

// Borrowed from https://gist.github.com/PeterPerhac/9a388f1d10945a2d6a29414ad79a8268
trait FutureAssertions {
  import org.scalatest.MustMatchers._
  import org.scalatest.concurrent.ScalaFutures._

  implicit class FutureReturns(f: Future[_]) {
    def returns(o: Any) = whenReady(f)(_ mustBe o)

    def failedWith(e: Exception) = whenReady(f.failed)(_ mustBe e)

    def failedWith[T <: Throwable](exClass: Class[T]) = whenReady(f.failed)(_.getClass mustBe exClass)
  }

  implicit class OptionTReturns[T](ot: OptionT[Future, T]) {

    def returnsSome(t: T) = whenReady(ot.value)(_ mustBe Some(t))

    def returnsNone = whenReady(ot.value)(_ mustBe Option.empty[T])

    def failedWith(e: Exception) = whenReady(ot.value.failed)(_ mustBe e)

  }
}