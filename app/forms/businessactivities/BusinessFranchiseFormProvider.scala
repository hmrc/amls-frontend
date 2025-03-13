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

package forms.businessactivities

import forms.mappings.Mappings
import models.businessactivities.{BusinessFranchise, BusinessFranchiseNo, BusinessFranchiseYes}
import play.api.data.Form
import play.api.data.Forms.mapping
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

import javax.inject.Inject

class BusinessFranchiseFormProvider @Inject() () extends Mappings {

  val length                           = 140
  def apply(): Form[BusinessFranchise] = Form[BusinessFranchise](
    mapping(
      "businessFranchise" -> boolean("error.required.ba.is.your.franchise", "error.required.ba.is.your.franchise"),
      "franchiseName"     -> mandatoryIfTrue(
        "businessFranchise",
        text("error.required.ba.franchise.name")
          .verifying(
            firstError(
              maxLength(length, "error.max.length.ba.franchise.name"),
              regexp(basicPunctuationRegex, "error.invalid.characters.ba.franchise.name")
            )
          )
      )
    )(apply)(unapply)
  )

  private def apply(b: Boolean, s: Option[String]): BusinessFranchise = (b, s) match {
    case (false, _)        => BusinessFranchiseNo
    case (true, Some(str)) => BusinessFranchiseYes(str)
    case _                 => throw new IllegalArgumentException("No franchise name available to bind from form")
  }

  private def unapply(obj: BusinessFranchise): Option[(Boolean, Option[String])] = obj match {
    case BusinessFranchiseNo           => Some((false, None))
    case BusinessFranchiseYes(details) => Some((true, Some(details)))
    case _                             => None
  }
}
