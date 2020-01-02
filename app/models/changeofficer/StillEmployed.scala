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

package models.changeofficer

import jto.validation.forms.UrlFormEncoded
import play.api.libs.json.Writes

sealed trait StillEmployed
case object StillEmployedYes extends StillEmployed
case object StillEmployedNo extends StillEmployed

object StillEmployed {
  import jto.validation._
  import utils.MappingUtils.Implicits._

  implicit val formReads: Rule[UrlFormEncoded, StillEmployed] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "stillEmployed").read[Boolean].withMessage("changeofficer.stillemployed.validationerror") map {
      case true => StillEmployedYes
      case false  => StillEmployedNo
    }
  }

  implicit val formWrites: Write[StillEmployed, UrlFormEncoded] = Write {
    case StillEmployedYes => "stillEmployed" -> "true"
    case StillEmployedNo => "stillEmployed" -> "false"
  }
}
