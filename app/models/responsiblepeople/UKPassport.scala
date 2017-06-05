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

package models.responsiblepeople

import cats.data.Validated.Valid
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Write}
import models.FormTypes
import play.api.libs.json._

sealed trait UKPassport

case class UKPassportYes(ukPassportNumber: String) extends UKPassport
case object UKPassportNo extends UKPassport

object UKPassport {

  import FormTypes._
  import jto.validation.forms.Rules._
  import utils.MappingUtils.Implicits._

  private val passportRegex = "^[0-9]{9}$".r
  private val passportRequired = required("error.required.uk.passport")
  private val passportInputLength = 9
  private val passportPattern = regexWithMsg(passportRegex, "error.invalid.uk.passport")
  private val passportLengthRule = maxLength(passportInputLength).withMessage("error.required.uk.passport") andThen
    minLength(passportInputLength).withMessage("error.required.uk.passport")

  val ukPassportType = notEmptyStrip andThen
    passportRequired andThen
    passportLengthRule andThen
    passportPattern

  implicit val formRule: Rule[UrlFormEncoded, UKPassport] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "ukPassport").read[Boolean].withMessage("error.required.uk.passport") flatMap {
      case true =>
        (__ \ "ukPassportNumber").read(ukPassportType) map (UKPassportYes apply _)
      case false => Rule.fromMapping { _ => Valid(UKPassportNo) }
    }
  }

  implicit val formWrites: Write[UKPassport, UrlFormEncoded] = Write {
    case UKPassportYes(value) =>
      Map(
        "ukPassport" -> Seq("true"),
        "ukPassportNumber" -> Seq(value)
      )
    case UKPassportNo => Map("ukPassport" -> Seq("false"))
  }

  implicit val jsonReads: Reads[UKPassport] = {
    (__ \ "ukPassport").read[Boolean] flatMap {
      case true => (__ \ "ukPassportNumber").read[String] map (UKPassportYes apply _)
      case false => Reads(_ => JsSuccess(UKPassportNo))
    }
  }

  implicit val jsonWrites = Writes[UKPassport] {
    case UKPassportYes(value) => Json.obj(
      "ukPassport" -> true,
      "ukPassportNumber" -> value
    )
    case UKPassportNo => Json.obj("ukPassport" -> false)
  }
}
