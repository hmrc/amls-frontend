/*
 * Copyright 2022 HM Revenue & Customs
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
import models.FormTypes._
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Write}
import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.domain.Nino

sealed trait Residency

case class UKResidence(nino: Nino) extends Residency

case object NonUKResidence extends Residency

object Residency {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, Residency] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "isUKResidence").read[Boolean].withMessage("error.required.rp.is.uk.resident") flatMap {
      case true => (__ \ "nino").read(ninoType).map(UKResidence.apply(_))
      case false => Rule.fromMapping { _ => Valid(NonUKResidence) }
    }
  }

  implicit def formWrites: Write[Residency, UrlFormEncoded] = Write {
    case UKResidence(nino) => Map(
      "isUKResidence" -> Seq("true"),
      "nino" -> Seq(nino.toString)
    )
    case NonUKResidence => Map(
      "isUKResidence" -> Seq("false")
    )
  }

  implicit val jsonReads: Reads[Residency] = {
    import play.api.libs.json._
    (__ \ "nino").read[Nino] map UKResidence.apply map identity[Residency] orElse Reads(_ => JsSuccess(NonUKResidence))
  }

  implicit val jsonWrites: Writes[Residency] = {
    import play.api.libs.json.Writes._
    import play.api.libs.json._
    Writes[Residency] {
      case UKResidence(nino) =>
        Json.obj(
          "nino" -> nino
        )
      case NonUKResidence => {
        Json.obj(
          "isUKResidence" -> "false"
        )
      }
    }
  }

}
