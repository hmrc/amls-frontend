/*
 * Copyright 2019 HM Revenue & Customs
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

import cats.data.Validated.{Invalid, Valid}
import jto.validation.{Path, Rule, ValidationError, Write}
import play.api.libs.json._

case class NonUKCountry(name: String, code: String) {
  override def toString: String = name
}

object NonUKCountry {

  implicit val writes = Writes[NonUKCountry] {
    case NonUKCountry(_, c) => JsString(c)
  }

  implicit val reads = Reads[NonUKCountry] {
    case JsString(code) =>
      nonUkcountries collectFirst {
        case e @ NonUKCountry(_, c) if c == code =>
          JsSuccess(e)
      } getOrElse {
        JsError(JsPath -> play.api.data.validation.ValidationError("error.invalid"))
      }
    case _ =>
      JsError(JsPath -> play.api.data.validation.ValidationError("error.invalid"))
  }

  implicit val formWrites: Write[NonUKCountry, String] =
    Write { _.code }

  implicit val formRule: Rule[String, NonUKCountry] =
    Rule {
      case "" => Invalid(Seq(Path -> Seq(ValidationError("error.required.country"))))
//      case "GB" => Invalid(Seq(Path -> Seq(ValidationError("error.required.non.uk.country"))))
      case code =>
        nonUkcountries.collectFirst {
          case e @ NonUKCountry(_, c) if c == code =>
            Valid(e)
        } getOrElse {
          Invalid(Seq(Path -> Seq(ValidationError("error.invalid.country"))))
        }
    }

  implicit val jsonW: Write[NonUKCountry, JsValue] = {
    import jto.validation.playjson.Writes.string
    formWrites andThen string
  }

  implicit val jsonR: Rule[JsValue, NonUKCountry] = {
    import jto.validation.playjson.Rules.stringR
    stringR andThen formRule
  }
}