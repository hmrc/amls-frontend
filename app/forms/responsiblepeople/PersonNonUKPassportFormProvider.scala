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
import models.responsiblepeople.{NoPassport, NonUKPassport, NonUKPassportYes}
import play.api.data.Form
import play.api.data.Forms.mapping
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

import javax.inject.Inject

class PersonNonUKPassportFormProvider @Inject()() extends Mappings {

  private val booleanFieldName = "nonUKPassport"
  private val emptyError = "error.required.non.uk.passport"

  val length = 40

  def apply(): Form[NonUKPassport] = Form[NonUKPassport](
    mapping(
      booleanFieldName -> boolean(emptyError, emptyError),
      "nonUKPassportNumber" -> mandatoryIfTrue(
        booleanFieldName,
        textAllowWhitespace(s"$emptyError.number")
          .verifying(
          firstError(
            maxLength(length, "error.invalid.non.uk.passport.number.length.40"),
            regexp(basicPunctuationRegex, "error.invalid.non.uk.passport.number")
          )
        )
      )
    )(apply)(unapply)
  )

  private def apply(nonUkPassport: Boolean, nonUkPassportNumber: Option[String]): NonUKPassport = (nonUkPassport, nonUkPassportNumber) match {
    case (true, Some(number)) => NonUKPassportYes(number)
    case (false, None) => NoPassport
    case _ => throw new IllegalArgumentException(s"Invalid combination of answers")
  }

  private def unapply(obj: NonUKPassport): Option[(Boolean, Option[String])] = obj match {
    case NonUKPassportYes(number) => Some((true, Some(number)))
    case NoPassport => Some((false, None))
  }
}
