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

import forms.behaviours.{CheckboxFieldBehaviours, StringFieldBehaviours}
import forms.mappings.Constraints
import models.declaration.release7.Partner
import play.api.data.FormError

class AddPersonFormProviderSpec extends CheckboxFieldBehaviours with StringFieldBehaviours with Constraints {

  val formProvider = new AddPersonFormProvider()
  val form         = formProvider()

  "AddPersonFormProvider" when {

    def validNameGen = stringsShorterThan(formProvider.nameLength).suchThat(_.nonEmpty)

    val nameFields = Seq(
      "first",
      "middle",
      "last"
    )

    nameFields.foreach { x =>
      val fieldName = x + "Name"

      s"$fieldName is evaluated" must {

        behave like fieldThatBindsValidData(form, fieldName, validNameGen)

        if (x != "middle") {
          behave like mandatoryField(
            form,
            fieldName,
            FormError(fieldName, s"error.required.declaration.${x}_name")
          )
        }

        behave like fieldWithMaxLength(
          form,
          fieldName,
          formProvider.nameLength,
          FormError(fieldName, s"error.invalid.${fieldName.toLowerCase}.length", Seq(formProvider.nameLength))
        )

        "not bind if regex is violated" in {

          val result = form.bind(
            (nameFields.filterNot(_ == x).map { x =>
              x + "Name" -> x
            } ++ Seq(
              fieldName      -> "☧",
              "positions[0]" -> Partner.toString
            )).toMap
          )

          result.value  shouldBe None
          result.errors shouldBe Seq(
            FormError(fieldName, s"error.invalid.${fieldName.toLowerCase}.validation", Seq(nameRegex))
          )
        }
      }
    }
  }
}
