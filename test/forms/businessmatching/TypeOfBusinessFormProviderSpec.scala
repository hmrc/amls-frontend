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

package forms.businessmatching

import forms.behaviours.StringFieldBehaviours
import forms.mappings.Constraints
import models.businessmatching.TypeOfBusiness
import play.api.data.FormError

class TypeOfBusinessFormProviderSpec extends StringFieldBehaviours with Constraints {

  lazy val formProvider = new TypeOfBusinessFormProvider()

  val fieldName = "typeOfBusiness"

  ".typeOfBusiness" must {

    behave like mandatoryField(
      formProvider(),
      fieldName,
      FormError(fieldName, "error.required.bm.businesstype.type")
    )

    "bind valid values" in {

      forAll(stringOfLengthGen(formProvider.maxLength)) { value =>
        formProvider().bind(Map(fieldName -> value)).value shouldBe Some(TypeOfBusiness(value))
      }
    }

    "fail to bind" when {

      s"value is longer than ${formProvider.maxLength}" in {

        formProvider().bind(Map(fieldName -> ("a" * (formProvider.maxLength + 1)))).errors shouldBe Seq(
          FormError(fieldName, "error.max.length.bm.businesstype.type", Seq(formProvider.maxLength))
        )
      }

      "value violates regex" in {

        formProvider().bind(Map(fieldName -> "⌧")).errors shouldBe Seq(
          FormError(fieldName, "error.bm.businesstype.type.characters", Seq(basicPunctuationRegex))
        )
      }
    }
  }
}
