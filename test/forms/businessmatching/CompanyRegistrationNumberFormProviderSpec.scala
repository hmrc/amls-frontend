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
import models.businessmatching.CompanyRegistrationNumber
import org.scalacheck.Gen
import play.api.data.FormError

class CompanyRegistrationNumberFormProviderSpec extends StringFieldBehaviours with Constraints {

  val form = new CompanyRegistrationNumberFormProvider()()
  val requiredMessage = "error.required.bm.registration.number"
  val lengthMessage = s"error.invalid.bm.registration.number.length"

  ".value" must {

    val fieldName = "value"

    val length = 8

    val companyRegNumberGen = for {
      str <- stringOfLengthGen(length)
    } yield str.toUpperCase

    val invalidStringGen = for {
      str <- companyRegNumberGen
      char <- Gen.oneOf(Seq('!', '@', '£', '$', '%', '^', '&', '*', '(', ')', ',', '.', '#', '`', '~', '<', '>', '?', '|', '"', '/', '[', ']', '\\'))
    } yield s"${str.dropRight(1)}$char".toUpperCase()

    "bind valid data" in {
      forAll(companyRegNumberGen) { companyRegNum =>
        val result = form.bind(Map(fieldName -> companyRegNum)).apply(fieldName)
        result.value shouldBe Some(companyRegNum)
      }
    }

    s"not bind strings with invalid characters" in {
      forAll(invalidStringGen) { invalidString =>
        val result = form.bind(Map(fieldName -> invalidString)).apply(fieldName)
        result.errors shouldEqual Seq(FormError(fieldName, Seq("error.invalid.bm.registration.number.allowed"), Seq("^[A-Z0-9]{8}$")))
      }
    }

    "contains whitespace" in {

      val validInputWithSpaces = "123 AB6 78"
      val trimmedInput = "123AB678"

      val result = form.bind(Map(fieldName -> validInputWithSpaces))

      result.value shouldBe Some(CompanyRegistrationNumber(trimmedInput))
      result.errors shouldBe Seq.empty
    }

    "not bind submissions with lower case letters" in {
      forAll(companyRegNumberGen) { companyRegNum =>
        val result = form.bind(Map(fieldName -> companyRegNum.toLowerCase)).apply(fieldName)
        result.errors shouldEqual Seq(FormError(fieldName, Seq("error.invalid.bm.registration.number.allowed"), Seq("^[A-Z0-9]{8}$")))
      }
    }

    s"not bind strings longer than $length characters" in {
      forAll(companyRegNumberGen, Gen.alphaNumChar) { (companyRegNum, char) =>
        val result = form.bind(Map(fieldName -> s"$companyRegNum$char")).apply(fieldName)
        result.errors shouldEqual Seq(FormError(fieldName, Seq(lengthMessage), Seq(length)))
      }
    }

    s"not bind strings shorter than $length characters" in {
      forAll(companyRegNumberGen) { companyRegNum =>
        val result = form.bind(Map(fieldName -> companyRegNum.dropRight(1))).apply(fieldName)
        result.errors shouldEqual Seq(FormError(fieldName, Seq(lengthMessage), Seq(length)))
      }
    }

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredMessage)
    )
  }
}
