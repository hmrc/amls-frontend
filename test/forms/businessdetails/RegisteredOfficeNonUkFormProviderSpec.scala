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

import forms.behaviours.AddressFieldBehaviours
import play.api.data.FormError

class RegisteredOfficeNonUkFormProviderSpec extends AddressFieldBehaviours {

  val formProvider = new RegisteredOfficeNonUkFormProvider()
  val form = formProvider()

  "RegisteredOfficeNonUkFormProvider" when {

    val addressLineList = Seq(1, 2, 3, 4) //TODO bump up to 1, 2, 3, 4 when all tests pass

    addressLineList foreach { line =>

      val fieldName = s"addressLine$line"

      s"Address Line $line is validated" must {

        val dataToBind: collection.mutable.Map[String, String] =
          collection.mutable.Map(addressLineList.filterNot(_ == line).map { l =>
            s"addressLine$l" -> s"Fake Address Line $l"
          }.toMap.toSeq: _*) += "country" -> "US"

        if (line == 1 || line == 2) {

          behave like mandatoryField(
            form,
            fieldName,
            FormError(fieldName, s"error.required.address.line$line")
          )
        }

        behave like fieldWithMaxLength(
          form,
          dataToBind,
          fieldName,
          formProvider.length,
          FormError(fieldName, s"error.max.length.address.line$line", Seq(formProvider.length))
        )

        behave like fieldWithRegexValidation(
          form,
          dataToBind,
          fieldName,
          formProvider.addressTypeRegex,
          FormError(fieldName, s"error.text.validation.address.line$line", Seq(formProvider.addressTypeRegex))
        )
      }
    }
  }
}
