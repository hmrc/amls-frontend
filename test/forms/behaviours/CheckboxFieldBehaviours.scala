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

package forms.behaviours

import play.api.data.{Form, FormError}

trait CheckboxFieldBehaviours extends FieldBehaviours {

  def checkboxField[T](form: Form[_], fieldName: String, validValues: Seq[T], invalidError: FormError): Unit = {
    for {
      (value, i) <- validValues.zipWithIndex
    } yield s"binds `$value` successfully" in {
      val data = Map(
        s"$fieldName[$i]" -> value.toString
      )
      form.bind(data).get shouldEqual Seq(value)
    }

    "fail to bind when the answer is invalid" in {
      val data = Map(
        s"$fieldName[0]" -> "invalid value"
      )
      form.bind(data).errors should contain(invalidError)
    }
  }

  def checkboxFieldWithWrapper[T, A](
    form: Form[_],
    fieldName: String,
    validValues: Seq[T],
    toWrapperSingle: T => A,
    toWrapperMulti: Seq[T] => A,
    invalidError: FormError,
    extraData: (String, String)*
  ): Unit = {
    for {
      (value, i) <- validValues.zipWithIndex
    } yield s"bind `$value` successfully" in {
      val data = (s"$fieldName[$i]" -> value.toString) +: extraData

      form.bind(data.toMap).get shouldEqual toWrapperSingle(value)
    }

    "bind all valid values at once" in {

      val data = for {
        (value, i) <- validValues.zipWithIndex
      } yield s"$fieldName[$i]" -> value.toString

      form.bind((data ++ extraData).toMap).get shouldEqual toWrapperMulti(validValues)
    }

    "fail to bind when the answer is invalid" in {
      val data = Map(
        s"$fieldName[0]" -> "invalid value"
      )
      form.bind(data).errors should contain(invalidError)
    }
  }

  def mandatoryCheckboxField(form: Form[_], fieldName: String, requiredKey: String): Unit = {

    "fail to bind when no answers are selected" in {
      val data = Map.empty[String, String]
      form.bind(data).errors should contain(FormError(s"$fieldName", requiredKey))
    }

    "fail to bind when blank answer provided" in {
      val data = Map(
        s"$fieldName[0]" -> ""
      )
      form.bind(data).errors should contain(FormError(s"$fieldName[0]", requiredKey))
    }
  }
}
