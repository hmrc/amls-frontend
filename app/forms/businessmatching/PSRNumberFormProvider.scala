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

package forms.businessmatching

import forms.mappings.Mappings
import models.businessmatching.{BusinessAppliedForPSRNumber, BusinessAppliedForPSRNumberNo, BusinessAppliedForPSRNumberYes}
import play.api.data.Form
import play.api.data.Forms.mapping
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

import javax.inject.Inject

class PSRNumberFormProvider @Inject() () extends Mappings {

  val min = 6
  val max = 7

  def apply(): Form[BusinessAppliedForPSRNumber] =
    Form[BusinessAppliedForPSRNumber](
      mapping(
        "appliedFor" -> boolean("error.required.msb.psr.options", "error.required.msb.psr.options"),
        "regNumber"  -> mandatoryIfTrue(
          "appliedFor",
          textAllowWhitespace("error.invalid.msb.psr.number")
            .verifying(
              firstError(
                minLength(min, "error.required.msb.psr.length"),
                maxLength(max, "error.required.msb.psr.length"),
                regexp("^[0-9]*$", "error.max.msb.psr.number.format")
              )
            )
        )
      )(apply)(unapply)
    )

  private def apply(b: Boolean, s: Option[String]): BusinessAppliedForPSRNumber = (b, s) match {
    case (false, _)        => BusinessAppliedForPSRNumberNo
    case (true, Some(str)) => BusinessAppliedForPSRNumberYes(str)
    case _                 => throw new IllegalArgumentException("No PSR Number available to bind from form")
  }

  private def unapply(obj: BusinessAppliedForPSRNumber): Option[(Boolean, Option[String])] = obj match {
    case BusinessAppliedForPSRNumberNo             => Some((false, None))
    case BusinessAppliedForPSRNumberYes(regNumber) => Some((true, Some(regNumber)))
    case _                                         => None
  }
}
