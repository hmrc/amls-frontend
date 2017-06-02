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

package models.responsiblepeople

import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule}
import models.Country
import play.api.libs.json.Json

case class CountryOfBirth (countryOfBirth: Boolean, country: Option[Country])

object CountryOfBirth {

  implicit val format = Json.format[CountryOfBirth]

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, CountryOfBirth] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      (__ \ "countryOfBirth").read[Boolean].withMessage("error.required.rp.select.country.of.birth") flatMap {
        case true => (__ \ "country").read[Country] map {c =>CountryOfBirth(true, Some(c))}
        case false => CountryOfBirth(false, None)
      }
    }

}