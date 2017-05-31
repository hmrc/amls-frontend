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

sealed trait NonUKPassport

case class NonUKPassportYes(passportNumberUk: String) extends NonUKPassport
case object NoPassport extends NonUKPassport

object NonUKPassport {

  import FormTypes._
  import jto.validation.forms.Rules._
  import utils.MappingUtils.Implicits._

  private val nonUKPassportRequired = required("error.required.non.uk.passport")
  private val nonUkPassportLength = maxWithMsg(maxNonUKPassportLength, "error.invalid.non.uk.passport")

  val noUKPassportType = notEmptyStrip andThen
    nonUKPassportRequired andThen
    nonUkPassportLength andThen
    basicPunctuationPattern("error.invalid.non.uk.passport")

  implicit val formRule: Rule[UrlFormEncoded, NonUKPassport] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "nonUKPassport").read[Boolean].withMessage("error.required.non.uk.passport") flatMap {
      case true =>
        (__ \ "nonUKPassportNumber").read(noUKPassportType) map (NonUKPassportYes apply _)
      case false => Rule.fromMapping { _ => Valid(NoPassport) }
    }
  }

  implicit val formWrites: Write[NonUKPassport, UrlFormEncoded] = Write {
    case NonUKPassportYes(value) =>
      Map(
        "nonUKPassport" -> Seq("true"),
        "nonUKPassportNumber" -> Seq(value)
      )
    case NoPassport => Map("nonUKPassport" -> Seq("false"))
  }

  implicit val jsonReads: Reads[NonUKPassport] =
    (__ \ "nonUKPassport").read[Boolean] flatMap {
      case true => (__ \ "nonUKPassportNumber").read[String] map (NonUKPassportYes apply _)
      case false => Reads(_ => JsSuccess(NoPassport))
    }

  implicit val jsonWrites = Writes[NonUKPassport] {
    case NonUKPassportYes(value) => Json.obj(
      "nonUKPassport" -> true,
      "nonUKPassportNumber" -> value
    )
    case NoPassport => Json.obj("nonUKPassport" -> false)
  }
}
