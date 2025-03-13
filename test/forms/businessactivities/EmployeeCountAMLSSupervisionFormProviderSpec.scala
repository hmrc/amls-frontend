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

package forms.businessactivities

import forms.behaviours.StringFieldBehaviours
import models.businessactivities.EmployeeCountAMLSSupervision
import play.api.data.{Form, FormError}

class EmployeeCountAMLSSupervisionFormProviderSpec extends StringFieldBehaviours {

  val formProvider: EmployeeCountAMLSSupervisionFormProvider = new EmployeeCountAMLSSupervisionFormProvider()
  val form: Form[EmployeeCountAMLSSupervision]               = formProvider()

  val fieldName = "employeeCountAMLSSupervision"

  "EmployeeCountAMLSSupervisionFormProvider" must {

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      numStringOfLength(formProvider.length)
    )

    behave like mandatoryField(
      form,
      fieldName,
      FormError(fieldName, "error.empty.ba.mlr.employee.count")
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      formProvider.length,
      FormError(fieldName, "error.max.length.ba.employee.count", Seq(formProvider.length))
    )

    "fail to bind non-numeric strings" in {

      forAll(alphaStringsShorterThan(formProvider.length).suchThat(_.nonEmpty)) { invalidStr =>
        val result = form.bind(Map(fieldName -> invalidStr))

        result.value                  shouldBe None
        result.error(fieldName).value shouldBe FormError(
          fieldName,
          "error.invalid.ba.employee.count",
          Seq(formProvider.regex)
        )
      }
    }
  }
}
