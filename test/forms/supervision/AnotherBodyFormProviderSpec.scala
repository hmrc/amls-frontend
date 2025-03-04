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

package forms.supervision

import forms.behaviours.BooleanFieldBehaviours
import forms.mappings.Constraints
import models.supervision.{AnotherBody, AnotherBodyNo, AnotherBodyYes}
import play.api.data.{Form, FormError}

class AnotherBodyFormProviderSpec extends BooleanFieldBehaviours[AnotherBody] with Constraints {

  val formProvider: AnotherBodyFormProvider = new AnotherBodyFormProvider()

  override val form: Form[AnotherBody] = formProvider()
  override val fieldName: String       = "anotherBody"
  override val errorMessage: String    = "error.required.supervision.anotherbody"

  val inputFieldName: String = "supervisorName"

  "form" must {

    "bind" when {

      "'No' is submitted" in {

        val boundForm = form.bind(Map(fieldName -> "false"))

        boundForm.value  shouldBe Some(AnotherBodyNo)
        boundForm.errors shouldBe Nil
      }

      "'Yes' is submitted and details are given" in {

        forAll(stringOfLengthGen(formProvider.length).suchThat(_.nonEmpty)) { name =>
          val boundForm = form.bind(Map(fieldName -> "true", inputFieldName -> name))

          boundForm.value  shouldBe Some(AnotherBodyYes(name))
          boundForm.errors shouldBe Nil
        }
      }
    }

    "fail to bind and give the correct error" when {

      "an empty value is submitted" in {

        val boundForm = form.bind(Map(fieldName -> ""))

        boundForm.errors.headOption shouldBe Some(FormError(fieldName, errorMessage))
      }

      "an invalid value is submitted" in {

        forAll(stringsLongerThan(1)) { invalidFormValue =>
          val boundForm = form.bind(Map(fieldName -> invalidFormValue))

          boundForm.errors.headOption shouldBe Some(FormError(fieldName, errorMessage))
        }
      }

      "'Yes' is submitted without a name" in {

        val boundForm = form.bind(Map(fieldName -> "true"))

        boundForm.errors.headOption shouldBe Some(FormError(inputFieldName, "error.required.supervision.supervisor"))
      }

      "'Yes' is submitted with a name" which {
        "is too long" in {

          forAll(stringsLongerThan(formProvider.length + 1)) { longName =>
            val boundForm = form.bind(Map(fieldName -> "true", inputFieldName -> longName))
            boundForm.errors.headOption shouldBe Some(
              FormError(inputFieldName, "error.invalid.supervision.supervisor.length.140", Seq(formProvider.length))
            )
          }
        }

        "is invalid" in {
          forAll(stringsShorterThan(formProvider.length - 1), invalidCharForNames) { (name, invalid) =>
            val boundForm = form.bind(Map(fieldName -> "true", inputFieldName -> (name + invalid)))
            boundForm.errors.headOption shouldBe Some(
              FormError(inputFieldName, "error.invalid.supervision.supervisor", Seq(basicPunctuationRegex))
            )
          }
        }
      }
    }
  }
}
