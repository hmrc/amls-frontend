/*
 * Copyright 2018 HM Revenue & Customs
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

package models.supervision

import jto.validation._
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json._
import cats.data.Validated.{Invalid, Valid}
import models.FormTypes._

sealed trait ProfessionalBody

case class ProfessionalBodyYes(value : String) extends ProfessionalBody
case object ProfessionalBodyNo extends ProfessionalBody


object ProfessionalBody {

  import utils.MappingUtils.Implicits._

  val maxPenalisedTypeLength = 255
  val penalisedType = notEmpty.withMessage("error.required.professionalbody.info.about.penalty") andThen
    maxLength(maxPenalisedTypeLength).withMessage("error.invalid.professionalbody.info.about.penalty") andThen
    basicPunctuationPattern()

  implicit val formRule: Rule[UrlFormEncoded, ProfessionalBody] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "penalised").read[Boolean].withMessage("error.required.professionalbody.penalised.by.professional.body") flatMap {
      case true =>
        (__ \ "professionalBody").read(penalisedType) map ProfessionalBodyYes.apply
      case false => Rule.fromMapping { _ => Valid(ProfessionalBodyNo) }
    }
  }

  implicit val formWrites: Write[ProfessionalBody, UrlFormEncoded] = Write {
    case ProfessionalBodyYes(value) =>
      Map("penalised" -> Seq("true"),
        "professionalBody" -> Seq(value)
      )
    case ProfessionalBodyNo => Map("penalised" -> Seq("false"))
  }

  implicit val jsonReads: Reads[ProfessionalBody] =
    (__ \ "penalised").read[Boolean] flatMap {
    case true => (__ \ "professionalBody").read[String] map ProfessionalBodyYes.apply _
    case false => Reads(_ => JsSuccess(ProfessionalBodyNo))
  }

  implicit val jsonWrites = Writes[ProfessionalBody] {
    case ProfessionalBodyYes(value) => Json.obj(
      "penalised" -> true,
      "professionalBody" -> value
    )
    case ProfessionalBodyNo => Json.obj("penalised" -> false)
  }
}

