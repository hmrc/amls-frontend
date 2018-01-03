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

package models.asp

import jto.validation._
import jto.validation.forms.Rules._
import play.api.libs.json.{Writes => _}
import utils.MappingUtils.Implicits._
import cats.data.Validated.{Invalid, Valid}
import jto.validation.forms.UrlFormEncoded

sealed trait OtherBusinessTaxMatters

case object OtherBusinessTaxMattersYes extends OtherBusinessTaxMatters

case object OtherBusinessTaxMattersNo extends OtherBusinessTaxMatters

object OtherBusinessTaxMatters {

  import play.api.libs.json._

  implicit val formRule: Rule[UrlFormEncoded, OtherBusinessTaxMatters] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "otherBusinessTaxMatters").read[Boolean].withMessage("error.required.asp.other.business.tax.matters") flatMap {
      case true => Rule.fromMapping { _ => Valid(OtherBusinessTaxMattersYes) }
      case false => Rule.fromMapping { _ => Valid(OtherBusinessTaxMattersNo) }
    }
  }

  implicit val formWrites: Write[OtherBusinessTaxMatters, UrlFormEncoded] = Write {
    case OtherBusinessTaxMattersYes => Map("otherBusinessTaxMatters" -> Seq("true"))
    case OtherBusinessTaxMattersNo => Map("otherBusinessTaxMatters" -> Seq("false"))
  }

  implicit val jsonReads: Reads[OtherBusinessTaxMatters] =
    (__ \ "otherBusinessTaxMatters").read[Boolean] flatMap {
      case true => Reads(__ => JsSuccess(OtherBusinessTaxMattersYes))
      case false => Reads(__ => JsSuccess(OtherBusinessTaxMattersNo))
    }

  implicit val jsonWrites = Writes[OtherBusinessTaxMatters] {
    case OtherBusinessTaxMattersYes => Json.obj("otherBusinessTaxMatters" -> true)
    case OtherBusinessTaxMattersNo => Json.obj("otherBusinessTaxMatters" -> false)
  }
}
