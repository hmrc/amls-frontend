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

import models.FormTypes
import models.estateagentbusiness.Other
import jto.validation._
import jto.validation.forms.UrlFormEncoded
import jto.validation.ValidationError
import play.api.libs.json._

sealed trait PassportType

case class UKPassport(passportNumberUk: String) extends PassportType
case class NonUKPassport(passportNumberNonUk: String) extends PassportType
case object NoPassport extends PassportType

object PassportType {

  import utils.MappingUtils.Implicits._
  import FormTypes._
  import jto.validation.forms.Rules._

  private val passportRegex = "^[0-9]{9}$".r
  private val passportRequired = required("error.required.uk.passport")
  private val passportInputLength = 9
  private val passportPattern = regexWithMsg(passportRegex, "error.invalid.uk.passport")
  private val passportLengthRule = maxLength(passportInputLength).withMessage("error.required.uk.passport") andThen
    minLength(passportInputLength).withMessage("error.required.uk.passport")

  private val nonUKPassportRequired = required("error.required.non.uk.passport")
  private val nonUkPassportLength = maxWithMsg(maxNonUKPassportLength, "error.invalid.non.uk.passport")

  val ukPassportType = notEmptyStrip andThen
    passportRequired andThen
    passportLengthRule andThen
    passportPattern
  val noUKPassportType = notEmptyStrip andThen
    nonUKPassportRequired andThen
    nonUkPassportLength andThen
    basicPunctuationPattern("error.invalid.non.uk.passport")

  implicit val formRule: Rule[UrlFormEncoded, PassportType] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    import models.FormTypes._
    (__ \ "passportType").read[String].withMessage("error.required.rp.passport.option") flatMap {
      case "01" =>
        (__ \ "ukPassportNumber").read(ukPassportType) map UKPassport.apply
      case "02" =>
        (__ \ "nonUKPassportNumber").read(noUKPassportType) map NonUKPassport.apply
      case "03" => NoPassport
      case _ =>
        (Path \ "passportType") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val formWrites: Write[PassportType, UrlFormEncoded] = Write {
    case UKPassport(ukNumber) =>  Map(
      "passportType" -> "01",
      "ukPassportNumber" -> ukNumber
    )
    case NonUKPassport(nonUKNumber) =>   Map(
      "passportType" -> "02",
      "nonUKPassportNumber" -> nonUKNumber
    )
    case NoPassport => "passportType" -> "03"
  }

  implicit val jsonReads : Reads[PassportType] = {
    import play.api.libs.json.Reads.StringReads
      (__ \ "passportType").read[String].flatMap[PassportType] {
        case "01" =>
          (__ \ "ukPassportNumber").read[String] map {
            UKPassport
          }
        case "02" =>
          (__ \ "nonUKPassportNumber").read[String] map {
            NonUKPassport
          }
        case "03" => NoPassport
        case _ =>
          play.api.data.validation.ValidationError("error.invalid")
      }
  }

  implicit val jsonWrites = Writes[PassportType] {
    case UKPassport(ukNumber) =>  Json.obj(
      "passportType" -> "01",
      "ukPassportNumber" -> ukNumber
    )
    case NonUKPassport(nonUKNumber) =>  Json.obj(
      "passportType" -> "02",
      "nonUKPassportNumber" -> nonUKNumber
    )
    case NoPassport => Json.obj("passportType" -> "03")
  }
}

