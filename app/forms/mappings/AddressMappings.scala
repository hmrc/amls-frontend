/*
 * Copyright 2023 HM Revenue & Customs
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

package forms.mappings

import models.Country
import play.api.data.Mapping
import play.api.data.validation.{Constraint, Invalid, Valid}

trait AddressMappings extends Mappings {

  val countryErrorKey: String

  val length: Int = 35
  val addressTypeRegex: String = "^[A-Za-z0-9 !'‘’\"“”(),./\u2014\u2013\u2010\u002d]{1,35}$"
  val postcodeRegex: String = "^[A-Za-z]{1,2}[0-9][0-9A-Za-z]?\\s?[0-9][A-Za-z]{2}$"
  private def requiredError(line: String) = s"error.required.address.$line"

  private def maxLengthError(line: String) = s"error.max.length.address.$line"

  private def regexError(line: String) = s"error.text.validation.address.$line"

  protected def addressLineMapping(line: String): Mapping[String] = {
    text(requiredError(line))
      .verifying(
        firstError(
          maxLength(length, maxLengthError(line)),
          regexp(addressTypeRegex, regexError(line))
        )
      )
  }

  protected def postcodeOrCountryMapping(isUKAddress: Boolean): (String, Mapping[String]) = {
    if (isUKAddress) {
      "postCode" -> text("error.required.postcode")
        .verifying(regexp(postcodeRegex, "error.invalid.postcode"))
    } else {
      "country" -> text("error.required.country").verifying(countryConstraint)
    }
  }

  protected def parseCountry(input: String): Country = {
    models.countries.collectFirst {
      case e @ Country(_, c) if c == input => e
    }.getOrElse(throw new IllegalArgumentException(s"Invalid country code submitted: $input"))
  }

  private def countryConstraint: Constraint[String] =
    Constraint {
      case str if ukOpt.contains(str) =>
        Invalid(countryErrorKey)
      case str if models.countries.map(_.code).contains(str) =>
        Valid
      case _ =>
        Invalid("error.invalid.country")
    }

  private val ukOpt = models.countries.collectFirst {
    case Country(value, code) if code == "GB" => code
  }
}
