/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.libs.json.{JsObject, JsValue, Json}

trait MappingUtils {

  import play.api.libs.json.{JsSuccess, Reads}

  def constant[A](a: A): Reads[A] = Reads(_ => JsSuccess(a))

  object Implicits {

    /*
     * Basic wrapping conversions to make writing mappings easier
     * Would be nice to be able to write these in a more `functional`
     * manner
     */
    implicit def toSeq[A](a: A): Seq[A] = Seq(a)

    implicit def toMap[A, B](t: (A, B)): Map[A, B] = Map(t)

    implicit def toMap2[A, B](t: (A, B)): Map[A, Seq[B]] = Map(t._1 -> Seq(t._2))

    /*
     * Json reads implicits
     */

    import play.api.libs.json.{JsError, JsSuccess, Reads}

    implicit def toReadsSuccess[A, B <: A](b: B): Reads[A] =
      Reads(_ => JsSuccess(b))

    implicit def toReadsFailure[A](f: play.api.libs.json.JsonValidationError): Reads[A] =
      Reads(_ => JsError(f))

  }

  object MonoidImplicits {
    import cats.Monoid
    implicit def jsonMonoid: Monoid[JsValue] = new Monoid[JsValue] {
      def combine(a1: JsValue, a2: JsValue): JsObject =
        a1.as[JsObject] deepMerge a2.as[JsObject]
      def empty: JsObject                             =
        Json.obj()
    }

    implicit def jsonObjMonoid: Monoid[JsObject] = new Monoid[JsObject] {
      def combine(a1: JsObject, a2: JsObject) =
        a1 deepMerge a2
      def empty: JsObject                     =
        Json.obj()
    }
  }
}

object MappingUtils extends MappingUtils
