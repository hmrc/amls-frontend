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

trait RadioFieldBehaviours extends FormSpec {

  def radioField[T](form: Form[_], fieldName: String, validValues: Seq[T], invalidError: FormError): Unit = {
    validValues.foreach { value =>
      s"binds `$value` successfully" in {
        val data = Map(fieldName -> value.toString)

        form.bind(data).get shouldEqual value
      }
    }

    "fail to bind when the answer is invalid" in {
      val data = Map(fieldName -> "invalid value")

      form.bind(data).errors should contain(invalidError)
    }
  }

  def mandatoryRadioField(form: Form[_], fieldName: String, requiredKey: String): Unit = {

    "fail to bind when no answers are selected" in {
      val data = Map.empty[String, String]
      form.bind(data).errors should contain(FormError(fieldName, requiredKey))
    }

    "fail to bind when blank answer provided" in {
      val data = Map(fieldName -> "")

      form.bind(data).errors should contain(FormError(fieldName, requiredKey))
    }
  }
}
