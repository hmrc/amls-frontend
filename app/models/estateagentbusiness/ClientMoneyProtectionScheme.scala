/*
 * Copyright 2020 HM Revenue & Customs
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

package models.estateagentbusiness

import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Success, Write}
import cats.data.Validated.{Invalid, Valid}
import play.api.libs.json._
import models.FormTypes._

sealed trait ClientMoneyProtectionScheme

case object ClientMoneyProtectionSchemeYes extends ClientMoneyProtectionScheme

case object ClientMoneyProtectionSchemeNo extends ClientMoneyProtectionScheme

object ClientMoneyProtectionScheme {

  import utils.MappingUtils.Implicits._

  implicit val jsonReads: Reads[ClientMoneyProtectionScheme] =
    (__ \ "clientMoneyProtection").read[Boolean] flatMap {
      case true  => Reads(_ => JsSuccess(ClientMoneyProtectionSchemeYes))
      case false => Reads(_ => JsSuccess(ClientMoneyProtectionSchemeNo))
    }

  implicit val jsonWrites = Writes[ClientMoneyProtectionScheme] {
    case ClientMoneyProtectionSchemeYes => Json.obj("clientMoneyProtection" -> true)
    case ClientMoneyProtectionSchemeNo  => Json.obj("clientMoneyProtection" -> false)
  }

}
