/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.libs.json._

sealed trait ProfessionalBodyMember

case object ProfessionalBodyMemberYes extends ProfessionalBodyMember

case object ProfessionalBodyMemberNo extends ProfessionalBodyMember

object ProfessionalBodyMember {

  implicit val jsonReads: Reads[ProfessionalBodyMember] =
    (__ \ "isAMember").read[Boolean] flatMap {
      case true  => Reads(_ => JsSuccess(ProfessionalBodyMemberYes))
      case false => Reads(_ => JsSuccess(ProfessionalBodyMemberNo))
    }

  implicit val jsonWrites: Writes[ProfessionalBodyMember] = Writes[ProfessionalBodyMember] {
    case ProfessionalBodyMemberYes => Json.obj("isAMember" -> true)
    case ProfessionalBodyMemberNo  => Json.obj("isAMember" -> false)
  }
}
