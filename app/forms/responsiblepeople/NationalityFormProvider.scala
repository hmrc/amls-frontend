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

import forms.mappings.AddressMappings
import models.Country
import models.responsiblepeople.{British, Nationality, OtherCountry}
import play.api.data.Form
import play.api.data.Forms.mapping
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfFalse

import javax.inject.Inject

class NationalityFormProvider @Inject() () extends AddressMappings {

  private val booleanFieldName = "nationality"
  private val emptyError       = "error.required.nationality"

  override val countryErrorKey: String = ""

  def apply(): Form[Nationality] = Form[Nationality](
    mapping(
      booleanFieldName -> boolean(emptyError, emptyError),
      "country"        -> mandatoryIfFalse(
        booleanFieldName,
        text("error.required.rp.nationality.country")
          .verifying(countryConstraint("error.invalid.rp.nationality.country"))
          .transform[Country](parseCountry, _.code)
      )
    )(apply)(unapply)
  )

  private def apply(isBritish: Boolean, country: Option[Country]): Nationality = (isBritish, country) match {
    case (true, None)           => British
    case (false, Some(country)) => OtherCountry(country)
    case _                      => throw new IllegalArgumentException(s"Invalid combination of answers")
  }

  private def unapply(obj: Nationality): Option[(Boolean, Option[Country])] = obj match {
    case British               => Some((true, None))
    case OtherCountry(country) => Some((false, Some(country)))
    case _                     => None
  }
}
