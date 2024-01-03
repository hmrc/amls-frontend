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

package forms.payments

import forms.behaviours.FieldBehaviours
import models.payments.WaysToPay
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class WaysToPayFormProviderSpec extends FieldBehaviours {

  val form: Form[WaysToPay] = new WaysToPayFormProvider()()
  val fieldName: String = "waysToPay"
  val errorMessage: String = "payments.waystopay.error"

  "WaysToPayFormProvider" must {

    behave like fieldThatBindsValidData(
      form, fieldName, Gen.oneOf(WaysToPay.all.map(_.toString))
    )

    "not bind when key is not present at all" in {
      checkForError(form, emptyForm, Seq(FormError(fieldName, errorMessage)))
    }

    "not bind blank values" in {
      checkForError(form, Map(s"$fieldName[0]" -> ""), Seq(FormError(fieldName, errorMessage)))
    }
  }
}
