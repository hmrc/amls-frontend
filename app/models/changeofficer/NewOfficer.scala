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

import play.api.libs.json.Json
import jto.validation.forms.UrlFormEncoded
import jto.validation._
import utils.MappingUtils.Implicits._


case class NewOfficer(name: String)

object NewOfficer {
  implicit val formats = Json.format[NewOfficer]

  implicit val formWrites = Write[NewOfficer, UrlFormEncoded] { data =>
    Map(
      "person" -> Seq(data.name)
    )
  }

  implicit val formReads: Rule[UrlFormEncoded, NewOfficer] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "person").read[String]
      .withMessage("changeofficer.newnominatedofficer.validationerror") map { p => NewOfficer(p) }
  }

}