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

package models.tradingpremises

import jto.validation.{From, Rule, Write}
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json.Json

case class ConfirmAddress(confirmAddress: Boolean)

object ConfirmAddress {

  implicit val formats = Json.format[ConfirmAddress]
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, ConfirmAddress] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      (__ \ "confirmAddress").read[Boolean].withMessage("error.required.tp.confirm.address") map ConfirmAddress.apply
    }

  implicit val formWrites: Write[ConfirmAddress, UrlFormEncoded] =
    Write {
      case ConfirmAddress(b) =>
        Map("confirmAddress" -> Seq(b.toString))
    }
}