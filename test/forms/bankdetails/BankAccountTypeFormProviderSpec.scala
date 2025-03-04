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

package forms.bankdetails

import forms.behaviours.FieldBehaviours
import models.bankdetails.BankAccountType._
import org.scalacheck.Gen
import play.api.data.FormError

class BankAccountTypeFormProviderSpec extends FieldBehaviours {

  val form      = new BankAccountTypeFormProvider()()
  val fieldName = "bankAccountType"
  val errorKey  = "error.bankdetails.accounttype"

  "BankAccountTypeFormProvider" must {

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      Gen.oneOf[String](Seq(PersonalAccount, BelongsToOtherBusiness, BelongsToBusiness).map(_.toString))
    )

    behave like mandatoryField(form, fieldName, FormError(fieldName, errorKey))

    "fail to bind invalid values" in {

      forAll(Gen.alphaNumStr) { invalidAnswer =>
        val result = form.bind(Map(fieldName -> invalidAnswer))

        result.value  shouldBe None
        result.errors shouldBe Seq(FormError(fieldName, errorKey))
      }
    }
  }
}
