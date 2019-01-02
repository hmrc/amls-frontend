/*
 * Copyright 2019 HM Revenue & Customs
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

package models.businessactivities

import jto.validation.{Write, From, Rule}
import jto.validation.forms._
import play.api.libs.json.Json

case class AccountantForAMLSRegulations(accountantForAMLSRegulations: Boolean)


object AccountantForAMLSRegulations {

  implicit val formats = Json.format[AccountantForAMLSRegulations]
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, AccountantForAMLSRegulations] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "accountantForAMLSRegulations").read[Boolean].withMessage("error.required.ba.business.use.accountant") map AccountantForAMLSRegulations.apply
  }

  implicit val formWrites: Write[AccountantForAMLSRegulations, UrlFormEncoded] = Write {
    case AccountantForAMLSRegulations(registered) => Map("accountantForAMLSRegulations" -> Seq(registered.toString))
  }
}
