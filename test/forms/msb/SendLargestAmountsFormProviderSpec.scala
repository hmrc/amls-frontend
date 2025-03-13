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

package forms.msb

import forms.behaviours.FieldBehaviours
import models.moneyservicebusiness.SendTheLargestAmountsOfMoney
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class SendLargestAmountsFormProviderSpec extends FieldBehaviours {
  val form: Form[SendTheLargestAmountsOfMoney] = new SendLargestAmountsFormProvider()()
  val fieldName: String                        = "largestAmountsOfMoney"
  val errorMessage: String                     = "error.invalid.countries.msb.sendlargestamount.country"

  "SendLargestAmountsFormProvider" must {

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      Gen.oneOf(models.countries.map(_.code))
    )

    "not bind when key is not present at all" in {
      checkForError(form, emptyForm, Seq(FormError(fieldName, errorMessage)))
    }

    "not bind blank values" in {
      checkForError(form, Map(s"$fieldName[0]" -> ""), Seq(FormError(fieldName, errorMessage)))
    }
  }
}
