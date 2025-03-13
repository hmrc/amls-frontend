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
import models.responsiblepeople.{Training, TrainingNo, TrainingYes}
import play.api.data.Form
import play.api.data.Forms.mapping
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

import javax.inject.Inject

class TrainingFormProvider @Inject() () extends Mappings {

  val length = 255

  private val booleanFieldName = "training"
  private val booleanError     = "error.required.rp.training"
  def apply(): Form[Training]  = Form[Training](
    mapping(
      booleanFieldName -> boolean(booleanError, booleanError),
      "information"    -> mandatoryIfTrue(
        booleanFieldName,
        text("error.required.rp.training.information").verifying(
          firstError(
            maxLength(length, "error.rp.invalid.training.information.maxlength.255"),
            regexp(basicPunctuationRegex, "error.rp.invalid.training.information")
          )
        )
      )
    )(apply)(unapply)
  )

  def apply(hasTraining: Boolean, information: Option[String]): Training =
    (hasTraining, information) match {
      case (true, Some(info)) => TrainingYes(info)
      case (false, None)      => TrainingNo
      case _                  => throw new IllegalArgumentException(s"Invalid combination of answers")
    }

  def unapply(obj: Training): Option[(Boolean, Option[String])] = obj match {
    case TrainingYes(info) => Some((true, Some(info)))
    case TrainingNo        => Some((false, None))
    case _                 => None
  }
}
