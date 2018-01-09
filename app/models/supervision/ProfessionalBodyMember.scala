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

package models.supervision

import jto.validation._
import jto.validation.forms.Rules.{minLength => _, _}
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json.Reads.StringReads
import play.api.libs.json._

sealed trait ProfessionalBodyMember

case object ProfessionalBodyMemberYes extends ProfessionalBodyMember

case object ProfessionalBodyMemberNo extends ProfessionalBodyMember

object ProfessionalBodyMember {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, ProfessionalBodyMember] =
    From[UrlFormEncoded] { __ =>
      (__ \ "isAMember").read[Boolean].withMessage("error.required.supervision.business.a.member") map {
        case true => ProfessionalBodyMemberYes
        case false => ProfessionalBodyMemberNo
      }
    }

  implicit def formWrites = Write[ProfessionalBodyMember, UrlFormEncoded] {
    case ProfessionalBodyMemberYes => Map("isAMember" -> "true")
    case ProfessionalBodyMemberNo => Map("isAMember" -> "false")
  }

  implicit val jsonReads: Reads[ProfessionalBodyMember] = {
    (__ \ "isAMember").read[Boolean] flatMap {
      case true => Reads(_ => JsSuccess(ProfessionalBodyMemberYes))
      case false => Reads(_ => JsSuccess(ProfessionalBodyMemberNo))
    }
  }

  implicit val jsonWrites = Writes[ProfessionalBodyMember] {
    case ProfessionalBodyMemberYes => Json.obj("isAMember" -> true)
    case ProfessionalBodyMemberNo => Json.obj("isAMember" -> false)

  }
}

