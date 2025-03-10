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

package models

import play.api.libs.json._

case class Country(name: String, code: String) {
  override def toString: String = name

  def isUK =
    this == Country("United Kingdom", "GB")

  def isEmpty =
    this == Country("", "")
}

object Country {

  val unitedKingdom: Country = Country("United Kingdom", "GB")

  implicit val writes: Writes[Country] = Writes[Country] { case Country(_, c) =>
    JsString(c)
  }

  implicit val reads: Reads[Country] = Reads[Country] {
    case JsString("")   => JsSuccess(Country("", ""))
    case JsString(code) =>
      countries collectFirst {
        case e @ Country(_, c) if c == code =>
          JsSuccess(e)
      } getOrElse {
        JsError(JsPath -> play.api.libs.json.JsonValidationError("error.invalid"))
      }
    case _              =>
      JsError(JsPath -> play.api.libs.json.JsonValidationError("error.invalid"))
  }
}
