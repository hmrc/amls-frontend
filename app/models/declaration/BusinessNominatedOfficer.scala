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

package models.declaration

import jto.validation.{From, Rule, To, Write}
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json.Json

case class BusinessNominatedOfficer(value: String)

object BusinessNominatedOfficer {

  import utils.MappingUtils.Implicits._

  val key = "business-nominated-officer"

  implicit val formRule: Rule[UrlFormEncoded, BusinessNominatedOfficer] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      (__ \ "value").read[String].withMessage("error.required.declaration.nominated.officer") map BusinessNominatedOfficer.apply
    }
  implicit val formWrites: Write[BusinessNominatedOfficer, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._
    (__ \ "value").write[String] contramap{x =>x.value}
  }

  implicit val format = Json.format[BusinessNominatedOfficer]

}
