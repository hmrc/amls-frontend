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

package models.aboutthebusiness

import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Write}
import play.api.libs.json.Json

case class ContactingYouPhone(phoneNumber: String)

object ContactingYouPhone {

  implicit val formats = Json.format[ContactingYouPhone]
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, ContactingYouPhone] =
  From[UrlFormEncoded] { __ =>
    import models.FormTypes._
    import jto.validation.forms.Rules._
      (__ \ "phoneNumber").read(phoneNumberType) map ContactingYouPhone.apply
  }

  implicit val formWrites: Write[ContactingYouPhone, UrlFormEncoded] =
    Write {
      case ContactingYouPhone(b) =>
        Map("phoneNumber" -> Seq(b.toString))
    }
}