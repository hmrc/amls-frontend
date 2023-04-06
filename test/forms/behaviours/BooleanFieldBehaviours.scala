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

import play.api.data.{Form, FormError}

trait BooleanFieldBehaviours[A] extends FieldBehaviours {

  val form: Form[A]
  val fieldName: String
  val errorMessage: String

  def booleanFieldWithModel(trueCase: A, falseCase: A): Unit = {

    "bind true" in {

      val result = form.bind(Map(fieldName -> "true"))

      result.value shouldBe Some(trueCase)
      result.errors shouldBe Nil
    }

    "bind false" in {

      val result = form.bind(Map(fieldName -> "false"))

      result.value shouldBe Some(falseCase)
      result.errors shouldBe Nil
    }

    "fail to bind" when {

      "given an invalid value" in {

        val result = form.bind(Map(fieldName -> "foo"))

        result.value shouldBe None
        result.errors shouldBe Seq(FormError(fieldName, errorMessage))
      }

      "given an empty value" in {

        val result = form.bind(Map(fieldName -> ""))

        result.value shouldBe None
        result.errors shouldBe Seq(FormError(fieldName, errorMessage))
      }
    }
  }

  def booleanField(form: Form[_],
                   fieldName: String,
                   invalidError: FormError): Unit = {

    "bind true" in {
      val result = form.bind(Map(fieldName -> "true"))
      result.value.value shouldBe true
    }

    "bind false" in {
      val result = form.bind(Map(fieldName -> "false"))
      result.value.value shouldBe false
    }

    "not bind non-booleans" in {

      forAll(nonBooleans -> "nonBoolean") {
        nonBoolean =>
          val result = form.bind(Map(fieldName -> nonBoolean)).apply(fieldName)
          result.errors shouldEqual Seq(invalidError)
      }
    }
  }
}
