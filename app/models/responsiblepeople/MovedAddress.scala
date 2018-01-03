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

package models.responsiblepeople

import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Write}
import play.api.libs.json.Json

case class MovedAddress(movedAddress: Boolean)

object MovedAddress {

  implicit val formats = Json.format[MovedAddress]
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, MovedAddress] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      (__ \ "movedAddress").read[Boolean].withMessage("error.required.rp.moved.address") map MovedAddress.apply
    }

  implicit val formWrites: Write[MovedAddress, UrlFormEncoded] =
    Write {
      case MovedAddress(b) =>
        Map("movedAddress" -> Seq(b.toString))
    }
}