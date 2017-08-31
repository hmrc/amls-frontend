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

package models.renewal

import jto.validation.forms.UrlFormEncoded

sealed trait UpdateInformation
case object UpdateInformationYes extends UpdateInformation
case object UpdateInformationNo extends UpdateInformation

object UpdateInformation {
  import jto.validation._
  import utils.MappingUtils.Implicits._

  implicit val formReads: Rule[UrlFormEncoded, UpdateInformation] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "updateInformation").read[Boolean].withMessage("changeofficer.updateinformation.validationerror") map {
      case true => UpdateInformationYes
      case false  => UpdateInformationNo
    }
  }

  implicit val formWrites: Write[UpdateInformation, UrlFormEncoded] = Write {
    case UpdateInformationYes => "updateInformation" -> "true"
    case UpdateInformationNo => "updateInformation" -> "false"
  }
}