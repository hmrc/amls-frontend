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
import models.responsiblepeople.{UKPassport, UKPassportNo, UKPassportYes}
import play.api.data.Form
import play.api.data.Forms.mapping
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

import javax.inject.Inject

class PersonUKPassportFormProvider @Inject()() extends Mappings {

  private val booleanFieldName = "ukPassport"
  private val booleanError = "error.required.uk.passport"
  private val lengthError = "error.invalid.uk.passport.length.9"

  private val regex = "^[0-9]{9}$"
  val length = 9

  def apply(): Form[UKPassport] = Form[UKPassport](
    mapping(
      booleanFieldName -> boolean(booleanError, booleanError),
      "ukPassportNumber" -> mandatoryIfTrue(
        booleanFieldName,
        textAllowWhitespace(s"$booleanError.number")
          .verifying(
            firstError(
              maxLength(length, lengthError),
              minLength(length, lengthError),
              regexp(regex, "error.invalid.uk.passport")
            )
          )
      )
    )(apply)(unapply)
  )

  private def apply(ukPassport: Boolean, ukPassportNumber: Option[String]): UKPassport = (ukPassport, ukPassportNumber) match {
    case (true, Some(number)) => UKPassportYes(number)
    case (false, None) => UKPassportNo
    case _ => throw new IllegalArgumentException(s"Invalid combination of answers")
  }


  private def unapply(obj: UKPassport): Option[(Boolean, Option[String])] = obj match {
    case UKPassportYes(number) => Some((true, Some(number)))
    case UKPassportNo => Some((false, None))
  }
}
