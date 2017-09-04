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

sealed trait UpdateAnyInformation
case object UpdateAnyInformationYes extends UpdateAnyInformation
case object UpdateAnyInformationNo extends UpdateAnyInformation

object UpdateAnyInformation {
  import jto.validation._
  import utils.MappingUtils.Implicits._

  implicit val formReads: Rule[UrlFormEncoded, UpdateAnyInformation] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "updateAnyInformation").read[Boolean].withMessage("error.renewal.updateanyInformation.validationerror") map {
      case true => UpdateAnyInformationYes
      case false  => UpdateAnyInformationNo
    }
  }

  implicit val formWrites: Write[UpdateAnyInformation, UrlFormEncoded] = Write {
    case UpdateAnyInformationYes => "updateAnyInformation" -> "true"
    case UpdateAnyInformationNo => "updateAnyInformation" -> "false"
  }
}