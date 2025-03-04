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
import models.responsiblepeople.{ExperienceTraining, ExperienceTrainingNo, ExperienceTrainingYes}
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.i18n.Messages
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

import javax.inject.Inject

class ExperienceTrainingFormProvider @Inject() () extends Mappings {

  val length = 255

  private val booleanFieldName                                                                                      = "experienceTraining"
  def apply(name: String, serviceOpt: Option[String] = None)(implicit messages: Messages): Form[ExperienceTraining] = {

    val booleanError = serviceOpt.fold(messages("error.required.rp.experiencetraining", name)) { service =>
      messages("error.required.rp.experiencetraining.one", name, service)
    }

    Form[ExperienceTraining](
      mapping(
        booleanFieldName        -> boolean(booleanError, booleanError),
        "experienceInformation" -> mandatoryIfTrue(
          booleanFieldName,
          text("error.required.rp.experiencetraining.information").verifying(
            firstError(
              maxLength(length, "error.rp.invalid.experiencetraining.information.maxlength.255"),
              regexp(basicPunctuationRegex, "error.rp.invalid.experiencetraining.information")
            )
          )
        )
      )(apply)(unapply)
    )
  }

  def apply(hasTraining: Boolean, information: Option[String]): ExperienceTraining =
    (hasTraining, information) match {
      case (true, Some(str)) => ExperienceTrainingYes(str)
      case (false, None)     => ExperienceTrainingNo
      case _                 => throw new IllegalArgumentException(s"Invalid combination of answers")
    }

  def unapply(obj: ExperienceTraining): Option[(Boolean, Option[String])] = obj match {
    case ExperienceTrainingYes(experienceInformation) => Some((true, Some(experienceInformation)))
    case ExperienceTrainingNo                         => Some((false, None))
    case _                                            => None
  }
}
