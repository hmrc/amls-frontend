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
import models.responsiblepeople.{NonUKResidence, PersonResidenceType, UKResidence}
import play.api.data.Form
import play.api.data.Forms.mapping
import uk.gov.hmrc.domain.Nino
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

import javax.inject.Inject

class PersonResidentTypeFormProvider @Inject() () extends Mappings {

  private val booleanFieldName = "isUKResidence"
  private val booleanError     = "error.required.rp.is.uk.resident"

  def apply(): Form[PersonResidenceType] = Form[PersonResidenceType](
    mapping(
      booleanFieldName -> boolean(booleanError, booleanError),
      "nino"           -> mandatoryIfTrue(
        booleanFieldName,
        text("error.required.nino")
          .transform[String](_.asNino, _.asNino)
          .verifying(validNino)
      )
    )(apply)(unapply)
  )

  private def apply(isUKResidence: Boolean, nino: Option[String]): PersonResidenceType = (isUKResidence, nino) match {
    case (true, Some(str)) => PersonResidenceType(UKResidence(Nino(str)), None, None)
    case (false, None)     => PersonResidenceType(NonUKResidence, None, None)
    case _                 => throw new IllegalArgumentException(s"Invalid combination of answers")
  }

  private def unapply(obj: PersonResidenceType): Option[(Boolean, Option[String])] = obj.isUKResidence match {
    case UKResidence(nino) => Some((true, Some(nino.value)))
    case NonUKResidence    => Some((false, None))
  }

  implicit class NinoStringFormatter(str: String) {
    def asNino: String = str.trim.replaceAll("-|\\s", "").toUpperCase
  }
}
