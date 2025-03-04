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
import models.responsiblepeople.ResponsiblePersonEndDate
import java.time.LocalDate
import play.api.data.Form
import play.api.data.Forms.mapping
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

import javax.inject.Inject

class RemoveResponsiblePersonFormProvider @Inject() () extends Mappings {

  private val booleanKey = "dateRequired"

  def apply(): Form[Either[Boolean, ResponsiblePersonEndDate]] = Form[Either[Boolean, ResponsiblePersonEndDate]](
    mapping(
      booleanKey -> boolean(),
      "endDate"  -> mandatoryIfTrue(
        booleanKey,
        localDate(
          oneInvalidKey = "error.invalid.rp.one",
          multipleInvalidKey = "error.invalid.rp.multiple",
          oneRequiredKey = "error.required.rp.one",
          twoRequiredKey = "error.required.rp.two",
          allRequiredKey = "error.required.rp.all",
          realDateKey = "error.invalid.rp.date.not.real"
        ).verifying(
          minDate(RemoveResponsiblePersonFormProvider.minDate, "error.invalid.year.post1900"),
          maxDate(RemoveResponsiblePersonFormProvider.maxDate, "error.invalid.rp.date.future")
        )
      )
    )(apply)(unapply)
  )

  private def apply(isRequired: Boolean, optDate: Option[LocalDate]): Either[Boolean, ResponsiblePersonEndDate] =
    (isRequired, optDate) match {
      case (true, Some(date)) => Right(ResponsiblePersonEndDate(date))
      case (false, None)      => Left(false)
    }

  private def unapply(either: Either[Boolean, ResponsiblePersonEndDate]): Option[(Boolean, Option[LocalDate])] =
    either match {
      case Left(value)  => Some((false, None))
      case Right(value) => Some((true, Some(value.endDate)))
    }
}

object RemoveResponsiblePersonFormProvider {

  val minDate: LocalDate = LocalDate.of(1900, 1, 1)

  def maxDate: LocalDate = LocalDate.now()
}
