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

package models.responsiblepeople

import cats.data.Validated.{Invalid, Valid}
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, ValidationError, Write}
import models.NonUKCountry

case class CountryOfBirth (bornInUk: Boolean, country: Option[NonUKCountry])

object CountryOfBirth {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, CountryOfBirth] = From[UrlFormEncoded] { __ =>
    val validateNonUKCountry: Rule[NonUKCountry, NonUKCountry] = Rule.fromMapping[NonUKCountry, NonUKCountry] {
      case country if country.code == "GB" => Invalid(Seq(ValidationError(List("error.required.atb.registered.office.uk.or.overseas"))))
      case country => Valid(country)
    }
      import jto.validation.forms.Rules._
      (__ \ "bornInUk").read[Boolean].withMessage("error.required.rp.select.country.of.birth") flatMap {
        case false => (__ \ "country").read(validateNonUKCountry.withMessage("error.required.enter.valid.non.uk")) map {
          c => CountryOfBirth(bornInUk = false, Some(c))
        }
        case true => CountryOfBirth(bornInUk = true, None)
      }
    }

  implicit val formWrites: Write[CountryOfBirth, UrlFormEncoded] = Write {x =>
    x.country match {
      case Some(country) =>  Map(
        "bornInUk" -> Seq("false"),
        "country" -> Seq(country.code)
      )
      case None =>  Map("bornInUk" -> Seq("true"))
    }
  }

}