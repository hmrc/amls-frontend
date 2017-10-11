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

package models.businessmatching.updateservice

import jto.validation.{From, Rule, Write}
import jto.validation.forms.UrlFormEncoded
import jto.validation.forms.Rules._

import utils.MappingUtils.Implicits._

sealed trait PassedFitAndProper

case object PassedFitAndProperYes extends PassedFitAndProper
case object PassedFitAndProperNo extends PassedFitAndProper

object PassedFitAndProper {

  implicit val formReads: Rule[UrlFormEncoded, PassedFitAndProper] = From[UrlFormEncoded] { __ =>
    (__ \ "passedFitAndProper").read[Boolean].withMessage("error.businessmatching.updateservice.fitandproper") map {
      case true => PassedFitAndProperYes
      case false => PassedFitAndProperNo
    }
  }

  implicit val formWrites: Write[PassedFitAndProper, UrlFormEncoded] = Write {
    case PassedFitAndProperYes => "passedFitAndProper" -> "true"
    case PassedFitAndProperNo => "passedFitAndProper" -> "false"
  }

}
