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

package models.changeofficer

import jto.validation.{From, Rule}
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json.Reads


case class RoleInBusiness(roles: Set[Role])

sealed trait Role

case object SoleProprietor extends Role
case object InternalAccountant extends Role

object RoleInBusiness {
  implicit def formReads:Rule[UrlFormEncoded, RoleInBusiness] = From[UrlFormEncoded] {
    __ => (__ \ "positions").read[String] map {
      case "06" => SoleProprietor
    }
  }
}