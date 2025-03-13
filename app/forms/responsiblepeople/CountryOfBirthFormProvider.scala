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
import models.responsiblepeople.CountryOfBirth
import play.api.data.Form
import play.api.data.Forms.mapping
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfFalse

import javax.inject.Inject

class CountryOfBirthFormProvider @Inject() () extends AddressMappings {

  private val booleanFieldName = "bornInUk"
  private val emptyError       = "error.required.rp.select.country.of.birth"

  override val countryErrorKey: String = "error.required.enter.valid.non.uk"

  def apply(): Form[CountryOfBirth] = Form[CountryOfBirth](
    mapping(
      booleanFieldName -> boolean(emptyError, emptyError),
      "country"        -> mandatoryIfFalse(
        booleanFieldName,
        text("error.required.rp.birth.country")
          .verifying(countryConstraintExcludeUK("error.invalid.rp.birth.country"))
          .transform[Country](parseCountry, _.code)
      )
    )(apply)(unapply)
  )

  private def apply(bornInUk: Boolean, country: Option[Country]): CountryOfBirth = (bornInUk, country) match {
    case (true, None)               => CountryOfBirth(bornInUk = true, None)
    case (false, country @ Some(_)) => CountryOfBirth(bornInUk = false, country)
    case _                          => throw new IllegalArgumentException(s"Invalid combination of answers")
  }

  private def unapply(obj: CountryOfBirth): Option[(Boolean, Option[Country])] = Some((obj.bornInUk, obj.country))
}
