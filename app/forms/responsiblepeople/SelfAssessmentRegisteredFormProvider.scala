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

package forms.responsiblepeople

import forms.mappings.Mappings
import models.responsiblepeople
import models.responsiblepeople.{SaRegistered, SaRegisteredNo, SaRegisteredYes}
import play.api.data.Form
import play.api.data.Forms.mapping
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

import javax.inject.Inject

class SelfAssessmentRegisteredFormProvider @Inject()() extends Mappings {

  private val booleanFieldName = "saRegistered"
  private val booleanError = "error.required.sa.registration"

  def apply(): Form[SaRegistered] = Form[responsiblepeople.SaRegistered](
    mapping(
      booleanFieldName -> boolean(booleanError, booleanError),
      "utrNumber" -> mandatoryIfTrue(
        booleanFieldName,
        text("error.required.utr.number").verifying(
          regexp(utrRegex, "error.invalid.length.utr.number")
        )
      )
    )(apply)(unapply)
  )

  private def apply(registeredForSA: Boolean, vrnNumber: Option[String]): SaRegistered = (registeredForSA, vrnNumber) match {
    case (true, Some(str)) => SaRegisteredYes(str)
    case (false, None) => SaRegisteredNo
    case _ => throw new IllegalArgumentException(s"Invalid combination of answers")
  }

  private def unapply(obj: SaRegistered): Option[(Boolean, Option[String])] = obj match {
    case SaRegisteredYes(vrnNumber) => Some((true, Some(vrnNumber)))
    case SaRegisteredNo => Some((false, None))
  }
}
