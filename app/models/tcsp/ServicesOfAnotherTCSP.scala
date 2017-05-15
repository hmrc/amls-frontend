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

package models.tcsp

import cats.data.Validated.Valid
import jto.validation._
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import models.FormTypes.referenceNumberRule
import play.api.libs.json._

sealed trait ServicesOfAnotherTCSP

case class ServicesOfAnotherTCSPYes(mlrRefNumber: String) extends ServicesOfAnotherTCSP

case object ServicesOfAnotherTCSPNo extends ServicesOfAnotherTCSP

object ServicesOfAnotherTCSP {

  import utils.MappingUtils.Implicits._

  val service = notEmpty.withMessage("error.invalid.mlr.number")
    .andThen(referenceNumberRule())

  implicit val formRule: Rule[UrlFormEncoded, ServicesOfAnotherTCSP] = From[UrlFormEncoded] { __ =>
  import jto.validation.forms.Rules._
    (__ \ "servicesOfAnotherTCSP").read[Boolean].withMessage("error.required.tcsp.services.another.tcsp") flatMap {
      case true =>
       (__ \ "mlrRefNumber").read(service) map ServicesOfAnotherTCSPYes.apply
      case false => Rule.fromMapping { _ => Valid(ServicesOfAnotherTCSPNo) }
    }
  }

  implicit val formWrites: Write[ServicesOfAnotherTCSP, UrlFormEncoded] = Write {
    case ServicesOfAnotherTCSPYes(value) =>
      Map("servicesOfAnotherTCSP" -> Seq("true"),
        "mlrRefNumber" -> Seq(value)
      )
    case ServicesOfAnotherTCSPNo => Map("servicesOfAnotherTCSP" -> Seq("false"))
  }

  implicit val jsonReads: Reads[ServicesOfAnotherTCSP] =
    (__ \ "servicesOfAnotherTCSP").read[Boolean] flatMap {
      case true => (__ \ "mlrRefNumber").read[String] map ServicesOfAnotherTCSPYes.apply
      case false => Reads(__ => JsSuccess(ServicesOfAnotherTCSPNo))
    }

  implicit val jsonWrites = Writes[ServicesOfAnotherTCSP] {
    case ServicesOfAnotherTCSPYes(value) => Json.obj(
          "servicesOfAnotherTCSP" -> true,
          "mlrRefNumber" -> value
    )
    case ServicesOfAnotherTCSPNo => Json.obj("servicesOfAnotherTCSP" -> false)
  }
}
