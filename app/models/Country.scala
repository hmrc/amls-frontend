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

package models

import jto.validation._
import jto.validation.ValidationError
import cats.data.Validated.{Invalid, Valid}
import play.api.libs.json._

case class Country(name: String, code: String) {
  override def toString: String = name
}

object Country {

  implicit val writes = Writes[Country] {
    case Country(_, c) => JsString(c)
  }

  implicit val reads = Reads[Country] {
    case JsString(code) =>
      countries collectFirst {
        case e @ Country(_, c) if c == code =>
          JsSuccess(e)
      } getOrElse {
        JsError(JsPath -> play.api.data.validation.ValidationError("error.invalid"))
      }
    case _ =>
      JsError(JsPath -> play.api.data.validation.ValidationError("error.invalid"))
  }

  implicit val formWrites: Write[Country, String] =
    Write { _.code }

  implicit val formRule: Rule[String, Country] =
    Rule {
      case "" => Invalid(Seq(Path -> Seq(ValidationError("error.required.country"))))
      case code =>
        countries.collectFirst {
          case e @ Country(_, c) if c == code =>
            Valid(e)
        } getOrElse {
          Invalid(Seq(Path -> Seq(ValidationError("error.invalid.country"))))
        }
    }

  implicit val jsonW: Write[Country, JsValue] = {
    import jto.validation.playjson.Writes.string
    formWrites andThen string
  }

  implicit val jsonR: Rule[JsValue, Country] = {
    import jto.validation.playjson.Rules.stringR
    stringR andThen formRule
  }
}
