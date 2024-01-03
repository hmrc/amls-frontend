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

package models.responsiblepeople

import cats.data.Validated.{Invalid, Valid}
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Path, Rule, ValidationError, Write}
import models.{Country, countries}

case class CountryOfBirth (bornInUk: Boolean, country: Option[Country])

object CountryOfBirth {

  import utils.MappingUtils.Implicits._

  val validateCountry: Rule[String, Country] = {
    Rule {
      case "" => Invalid(Seq(Path -> Seq(ValidationError("error.required.rp.birth.country"))))
      case "GB" => Invalid(Seq(Path -> Seq(ValidationError("error.required.enter.valid.non.uk"))))
      case code =>
        countries.collectFirst {
          case e @ Country(_, c) if c == code =>
            Valid(e)
        } getOrElse {
          Invalid(Seq(Path -> Seq(ValidationError("error.invalid.rp.birth.country"))))
        }
    }
  }

  implicit val formRule: Rule[UrlFormEncoded, CountryOfBirth] = From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      (__ \ "bornInUk").read[Boolean].withMessage("error.required.rp.select.country.of.birth") flatMap {
        case false => (__ \ "country").read(validateCountry) map {
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