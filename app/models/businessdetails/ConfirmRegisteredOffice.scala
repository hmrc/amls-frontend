/*
 * Copyright 2023 HM Revenue & Customs
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

package models.businessdetails

import jto.validation.forms._
import jto.validation.{From, Rule, Write}
import play.api.libs.json.{Json, OFormat}

case class ConfirmRegisteredOffice(isRegOfficeOrMainPlaceOfBusiness: Boolean)

object ConfirmRegisteredOffice {

  implicit val formats: OFormat[ConfirmRegisteredOffice] = Json.format[ConfirmRegisteredOffice]
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, ConfirmRegisteredOffice] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      (__ \ "isRegOfficeOrMainPlaceOfBusiness").read[Boolean].withMessage("error.required.atb.confirm.office") map ConfirmRegisteredOffice.apply
    }

  implicit val formWrites: Write[ConfirmRegisteredOffice, UrlFormEncoded] =
    Write {
      case ConfirmRegisteredOffice(b) =>
        Map("isRegOfficeOrMainPlaceOfBusiness" -> Seq(b.toString))
    }
}
