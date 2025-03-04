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

package forms.generic

import forms.mappings.AddressMappings
import play.api.data.Form
import play.api.data.Forms.{mapping, optional}

trait CorrespondenceAddressFormProvider[A] extends AddressMappings {

  def toObject: (String, String, String, Option[String], Option[String], Option[String], String) => A

  def fromObject: A => Option[(String, String, String, Option[String], Option[String], Option[String], String)]

  val nameMaxLength         = 140
  val businessNameMaxLength = 120

  val regex: String =
    "^[a-zA-Z0-9\u00C0-\u00FF !#$%&'‘’\"“”«»()*+,./:;=?@\\[\\]|~£€¥\\u005C\u2014\u2013\u2010\u005F\u005E\u0060\u000A\u000D\u002d]+$"

  def createForm(isUKAddress: Boolean): Form[A] = Form[A](
    mapping(
      "yourName"     -> text("error.required.yourname").verifying(
        firstError(
          maxLength(nameMaxLength, "error.invalid.yourname"),
          regexp(regex, "error.invalid.yourname.validation")
        )
      ),
      "businessName" -> text("error.required.name.of.business").verifying(
        firstError(
          maxLength(businessNameMaxLength, "error.invalid.name.of.business"),
          regexp(regex, "error.invalid.name.of.business.validation")
        )
      ),
      "addressLine1" -> addressLineMapping("line1"),
      "addressLine2" -> optional(addressLineMapping("line2")),
      "addressLine3" -> optional(addressLineMapping("line3")),
      "addressLine4" -> optional(addressLineMapping("line4")),
      postcodeOrCountryMapping(isUKAddress)
    )(toObject)(fromObject)
  )
}
