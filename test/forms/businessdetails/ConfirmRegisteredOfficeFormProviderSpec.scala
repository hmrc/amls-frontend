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

package forms.businessdetails

import forms.behaviours.BooleanFieldBehaviours
import models.businessdetails.ConfirmRegisteredOffice
import play.api.data.{Form, FormError}

class ConfirmRegisteredOfficeFormProviderSpec extends BooleanFieldBehaviours {

  val form: Form[ConfirmRegisteredOffice] = new ConfirmRegisteredOfficeFormProvider()()
  val fieldName: String = "isRegOfficeOrMainPlaceOfBusiness"

  def map(str: String): Map[String, String] = Map(fieldName -> str)

  "ConfirmRegisteredOfficeFormProvider" must {

    "bind true" in {

      form.bind(map("true")).value shouldBe Some(ConfirmRegisteredOffice(true))
    }

    "bind false" in {

      form.bind(map("false")).value shouldBe Some(ConfirmRegisteredOffice(false))
    }

    "fail to bind" when {

      "given an invalid value" in {

        form.bind(map("foo")).errors shouldBe Seq(FormError(fieldName, "error.required.atb.confirm.office"))
      }

      "given an empty value" in {

        form.bind(map("")).errors shouldBe Seq(FormError(fieldName, "error.required.atb.confirm.office"))
      }
    }
  }
}
