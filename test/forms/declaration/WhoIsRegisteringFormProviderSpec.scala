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

package forms.declaration

import forms.behaviours.StringFieldBehaviours
import models.declaration.WhoIsRegistering
import play.api.data.{Form, FormError}

class WhoIsRegisteringFormProviderSpec extends StringFieldBehaviours {

  val formProvider                              = new WhoIsRegisteringFormProvider()
  def form(str: String): Form[WhoIsRegistering] = formProvider(str)

  val fieldName   = "person"
  val errorPrefix = "error.required.declaration.who.is.declaring.this"

  "WhoIsRegisteringFormProvider" must {

    "bind with valid data" in {

      val str    = "partner1"
      val result = form("renewal").bind(Map(fieldName -> str))

      result.value shouldBe Some(WhoIsRegistering(str))
      assert(result.errors.isEmpty)
    }

    "append given string to error message" in {
      val suffix = "foo"
      formProvider(suffix).bind(Map(fieldName -> "")).error(fieldName).map(_.message) shouldBe Some(
        s"$errorPrefix.$suffix"
      )
    }

    behave like mandatoryField(form("renewal"), fieldName, FormError(fieldName, s"$errorPrefix.renewal"))
  }
}
