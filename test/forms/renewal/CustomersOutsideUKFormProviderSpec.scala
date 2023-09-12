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

package forms.renewal

import forms.behaviours.FieldBehaviours
import models.renewal.CustomersOutsideUK
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class CustomersOutsideUKFormProviderSpec extends FieldBehaviours {
  val form: Form[CustomersOutsideUK] = new CustomersOutsideUKFormProvider()()
  val fieldName: String = "countries"
  val errorMessage: String = "error.required.renewal.customer.country.name"

  "MostTransactionsFormProvider" must {

    behave like fieldThatBindsValidData(
      form, fieldName, Gen.oneOf(models.countries.map(_.code))
    )

    "not bind when key is not present at all" in {
      checkForError(form, emptyForm, Seq(FormError(fieldName, errorMessage)))
    }

    "not bind blank values" in {
      checkForError(form, Map(s"$fieldName[0]" -> ""), Seq(FormError(fieldName, errorMessage)))
    }
  }
}
