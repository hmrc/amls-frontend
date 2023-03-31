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

package forms.generic

import forms.mappings.Mappings
import models.{Country, countries}
import play.api.data.Forms.{mapping, optional}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.data.{Form, Mapping}

trait AddressFormProvider[A] extends Mappings {

  val countryErrorKey: String

  def toObject: (String, String, Option[String], Option[String], String) => A

  def fromObject: A => Option[(String, String, Option[String], Option[String], String)]

  val length: Int = 35
  val addressTypeRegex: String = "^[A-Za-z0-9 !'‘’\"“”(),./\u2014\u2013\u2010\u002d]{1,35}$"
  val postcodeRegex: String = "^[A-Za-z]{1,2}[0-9][0-9A-Za-z]?\\s?[0-9][A-Za-z]{2}$"

  private val ukOpt = countries.collectFirst {
    case Country(value, code) if code == "GB" => code
  }

  def createForm(isUKAddress: Boolean): Form[A] = Form[A](
    mapping(
      "addressLine1" -> addressLineMapping("line1"),
      "addressLine2" -> addressLineMapping("line2"),
      "addressLine3" -> optional(addressLineMapping("line3")),
      "addressLine4" -> optional(addressLineMapping("line4")),
      postcodeOrCountryMapping(isUKAddress)
    )(toObject)(fromObject)
  )

  private def requiredError(line: String) = s"error.required.address.$line"
  private def maxLengthError(line: String) = s"error.max.length.address.$line"
  private def regexError(line: String) = s"error.text.validation.address.$line"

  private def addressLineMapping(line: String): Mapping[String] = {
    text(requiredError(line))
      .verifying(
        firstError(
          maxLength(length, maxLengthError(line)),
          regexp(addressTypeRegex, regexError(line))
        )
      )
  }
  private def postcodeOrCountryMapping(isUKAddress: Boolean): (String, Mapping[String]) = {
    if(isUKAddress){
      "postcode" -> text("error.required.postcode")
        .verifying(regexp(postcodeRegex, "error.invalid.postcode"))
    } else {
      "country" -> text("error.required.country").verifying(countryConstraint)
    }
  }

  private def countryConstraint: Constraint[String] =
    Constraint {
      case str if ukOpt.contains(str) =>
        Invalid(countryErrorKey)
      case str if countries.map(_.code).contains(str) =>
        Valid
      case _ =>
        Invalid("error.invalid.country")
    }
}
