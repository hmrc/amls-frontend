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

package forms.behaviours

import models.Country
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

import collection.mutable.{Map => MutableMap}


trait AddressFieldBehaviours extends FieldBehaviours {

  val form: Form[_]
  val maxLength: Int
  val regexString: String

  def formWithAddressFields(requiredErrorKey: String, lengthErrorKey: String, regexErrorKey: String): Unit = {

    val addressLineList = (1 to 4).toList

    addressLineList foreach { line =>

      val fieldName = s"addressLine$line"

      s"Address Line $line is validated" must {

        val dataToBind: collection.mutable.Map[String, String] =
          collection.mutable.Map(addressLineList.filterNot(_ == line).map { l =>
            s"addressLine$l" -> s"Fake Address Line $l"
          }.toMap.toSeq: _*) += "country" -> "US"

        "must bind correctly" in {

          forAll(Gen.alphaNumStr.suchThat(_.length <= maxLength)) { addressLine =>

            val formData: MutableMap[String, String] = dataToBind += (fieldName -> addressLine)
            val newForm = form.bind(Map(formData.toSeq.sortBy(_._1): _*))

            newForm(fieldName).value shouldBe Some(addressLine)
          }
        }

        if (line == 1 || line == 2) {

          behave like mandatoryField(
            form,
            fieldName,
            FormError(fieldName, s"$requiredErrorKey.line$line")
          )
        }

        behave like fieldWithMaxLength(
          dataToBind,
          fieldName,
          FormError(fieldName, s"$lengthErrorKey.line$line", Seq(maxLength))
        )

        behave like fieldWithRegexValidation(
          dataToBind,
          fieldName,
          FormError(fieldName, s"$regexErrorKey.line$line", Seq(regexString))
        )
      }
    }
  }

  def fieldWithMaxLength(extraData: MutableMap[String, String],
                         fieldName: String,
                         lengthError: FormError): Unit = {

    s"not bind strings longer than $maxLength characters" in {

      forAll(Gen.alphaNumStr.suchThat(_.length > maxLength)) { string =>

        val formData: MutableMap[String, String] = extraData += (fieldName -> string)
        val newForm = form.bind(Map(formData.toSeq.sortBy(_._1): _*))

        newForm(fieldName).errors shouldEqual Seq(lengthError)
      }
    }
  }
  def fieldWithRegexValidation(extraData: MutableMap[String, String],
                               fieldName: String,
                               regexError: FormError): Unit = {

    s"not bind strings that violate regex" in {

      val invalidAddressLines = List(
        "WXN\"XYd*`Zvigcpmip7t",
        "E8TplR(!:FnxTmZ9{eSni+^.%ln)",
        "(?YJ.M2^OAJ<!AXM%kp",
        "@DzziLxs^k|~fXC}z]#EIHi?5Xwzn",
        ",7&2V X~Ksa!U;",
        ".zxtNH+Z,#xon1slaz3bwU2\"XC*[<",
        "/BUWD.-%LiY1Wj7uq%0R^s",
        "Ii+VI[VMpUJ2UJPXC"
      )

      forAll(Gen.oneOf(invalidAddressLines)) { line =>

        val formData: MutableMap[String, String] = extraData += (fieldName -> line)
        val newForm = form.bind(Map(formData.toSeq.sortBy(_._1): _*))

        newForm(fieldName).errors shouldEqual Seq(regexError)
      }
    }
  }

  def countryField(isUKErrorKey: String): Unit = {

    val countryField = "country"
    val uk = Country("United Kingdom", "GB")

    val extraData = collection.mutable.Map(
      "addressLine1" -> "123 Test Street",
      "addressLine2" -> "Test Village",
      "addressLine3" -> "Test City",
      "addressLine4" -> "Test County"
    )

    "country is validated" must {

      "bind a valid country" in {

        forAll(Gen.oneOf(models.countries)) { country =>

          val formData: MutableMap[String, String] = extraData += (countryField -> country.code)
          val newForm = form.bind(Map(formData.toSeq.sortBy(_._1): _*))

          newForm(countryField).value shouldBe Some(country.code)
        }
      }

      "fail to bind" when {

        "the given country is UK" in {

          val formData: MutableMap[String, String] = extraData += (countryField -> uk.code)
          val newForm = form.bind(Map(formData.toSeq.sortBy(_._1): _*))

          newForm(countryField).error shouldBe Some(FormError(countryField, isUKErrorKey))
        }

        "country is empty" in {

          val formData: MutableMap[String, String] = extraData += (countryField -> "")
          val newForm = form.bind(Map(formData.toSeq.sortBy(_._1): _*))

          newForm(countryField).error shouldBe Some(FormError(countryField, "error.required.country"))
        }

        "country is invalid" in {

          val formData: MutableMap[String, String] = extraData += (countryField -> "foo")
          val newForm = form.bind(Map(formData.toSeq.sortBy(_._1): _*))

          newForm(countryField).error shouldBe Some(FormError(countryField, "error.invalid.country"))
        }
      }
    }
  }
}
