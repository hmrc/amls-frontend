/*
 * Copyright 2021 HM Revenue & Customs
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

package models.declaration

import jto.validation._
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json._

sealed trait RenewRegistration

case object RenewRegistrationYes extends RenewRegistration
case object RenewRegistrationNo extends RenewRegistration

object RenewRegistration {
  import utils.MappingUtils.Implicits._

  val key = "declaration"

  implicit val formRule: Rule[UrlFormEncoded, RenewRegistration] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "renewRegistration").read[Boolean].withMessage("error.required.declaration.renew.registration") flatMap {
      case true  => Rule.fromMapping { _ => Valid(RenewRegistrationYes) }
      case false => Rule.fromMapping { _ => Valid(RenewRegistrationNo) }
    }
  }

  implicit val formWrites: Write[RenewRegistration, UrlFormEncoded] = Write {
    case RenewRegistrationYes => Map("renewRegistration" -> Seq("true"))
    case RenewRegistrationNo  => Map("renewRegistration" -> Seq("false"))
  }

  implicit val jsonReads: Reads[RenewRegistration] =
    (__ \ "renewRegistration").read[Boolean] flatMap {
      case true =>  Reads(_ => JsSuccess(RenewRegistrationYes))
      case false => Reads(_ => JsSuccess(RenewRegistrationNo))
    }

  implicit val jsonWrites = Writes[RenewRegistration] {
    case RenewRegistrationYes => Json.obj("renewRegistration" -> true)
    case RenewRegistrationNo  => Json.obj("renewRegistration" -> false)
  }
}


