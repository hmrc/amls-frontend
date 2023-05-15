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

package forms.supervision

import forms.mappings.Mappings
import models.supervision.{ProfessionalBody, ProfessionalBodyNo, ProfessionalBodyYes}
import play.api.data.Form
import play.api.data.Forms.mapping
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

import javax.inject.Inject

class PenalisedByProfessionalFormProvider @Inject()() extends Mappings {

  val length = 255

  private val booleanField = "penalised"
  private val booleanError = "error.required.professionalbody.penalised.by.professional.body"

  def apply(): Form[ProfessionalBody] = Form[ProfessionalBody](
    mapping(
      booleanField -> boolean(booleanError, booleanError),
      "professionalBody" -> mandatoryIfTrue(
        booleanField, text("error.required.professionalbody.info.about.penalty").verifying(
          firstError(
            maxLength(length, "error.invalid.professionalbody.info.about.penalty.length.255"),
            regexp(basicPunctuationRegex, "error.invalid.professionalbody.info.about.penalty")
          )
        )
      )
    )(apply)(unapply)
  )

  private def apply(penalised: Boolean, professionalBody: Option[String]): ProfessionalBody = (penalised, professionalBody) match {
    case (true, Some(body)) => ProfessionalBodyYes(body)
    case (false, _) => (ProfessionalBodyNo)
    case _ => throw new IllegalArgumentException("Professional body required")
  }

  private def unapply(obj: ProfessionalBody): Option[(Boolean, Option[String])] = obj match {
    case ProfessionalBodyYes(value) => Some((true, Some(value)))
    case ProfessionalBodyNo => Some((false, None))
  }
}
