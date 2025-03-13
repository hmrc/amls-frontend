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

import forms.behaviours.StringFieldBehaviours
import models.responsiblepeople.PersonName
import play.api.data.{Form, FormError}

class PersonNameFormProviderSpec extends StringFieldBehaviours {

  val formProvider           = new PersonNameFormProvider()
  val form: Form[PersonName] = formProvider()

  val nameRegex = "^[a-zA-Z\\u00C0-\\u00FF '‘’\\u2014\\u2013\\u2010\\u002d]+$"

  def default: String = alphaStringsShorterThan(formProvider.length).sample.getOrElse("Name")

  "PersonNameFormProvider" when {

    val list = List("first", "middle", "last")

    list foreach { field =>
      val fieldName = s"${field}Name"

      s"evaluating $field name field" must {

        behave like fieldThatBindsValidData(
          form,
          fieldName,
          stringsShorterThan(formProvider.length).suchThat(_.nonEmpty)
        )

        if (field != "middle") {
          behave like mandatoryField(form, fieldName, FormError(fieldName, s"error.required.rp.${field}_name"))
        }

        behave like fieldWithMaxLength(
          form,
          fieldName,
          formProvider.length,
          FormError(fieldName, s"error.invalid.rp.${field}_name.length", Seq(formProvider.length))
        )

        "reject submissions that violate regex" in {

          val otherFields = list.filterNot(_ == field)

          val map = (otherFields.map(x => x -> default) :+ (fieldName -> "!$£)(^&@{:|}{*§~`")).toMap

          val result = form.bind(map)

          result.value            shouldBe None
          result.error(fieldName) shouldBe Some(
            FormError(fieldName, s"error.invalid.rp.${field}_name.validation", Seq(nameRegex))
          )
        }
      }
    }
  }
}
