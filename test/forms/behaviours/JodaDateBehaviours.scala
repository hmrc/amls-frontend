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

import org.joda.time.LocalDate
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class JodaDateBehaviours extends FieldBehaviours {

  def dateField(form: Form[_], key: String, validData: Gen[LocalDate]): Unit = {

    "bind valid data" in {

      forAll(validData -> "valid date") {
        date =>

          val data = Map(
            s"$key.day"   -> date.getDayOfMonth.toString,
            s"$key.month" -> date.getMonthOfYear.toString,
            s"$key.year"  -> date.getYear.toString
          )

          val result = form.bind(data)

          result.value.value shouldEqual date
      }
    }
  }

  def dateFieldWithMax(form: Form[_], key: String, max: LocalDate, formError: FormError): Unit = {

    s"fail to bind a date greater than ${max.getDayOfMonth}/${max.getMonthOfYear}/${max.getYear}" in {

      val generator = jodaDatesBetween(max.plusDays(1), max.plusYears(10))

      forAll(generator -> "invalid dates") {
        date =>

          val data = Map(
            s"$key.day"   -> date.getDayOfMonth.toString,
            s"$key.month" -> date.getMonthOfYear.toString,
            s"$key.year"  -> date.getYear.toString
          )

          val result = form.bind(data)

          result.errors should contain only formError
      }
    }
  }

  def dateFieldWithMin(form: Form[_], key: String, min: LocalDate, formError: FormError): Unit = {

    s"fail to bind a date earlier than ${min.getDayOfMonth}/${min.getMonthOfYear}/${min.getYear}" in {

      val generator = jodaDatesBetween(min.minusYears(10), min.minusDays(1))

      forAll(generator -> "invalid dates") {
        date =>

          val data = Map(
            s"$key.day"   -> date.getDayOfMonth.toString,
            s"$key.month" -> date.getMonthOfYear.toString,
            s"$key.year"  -> date.getYear.toString
          )

          val result = form.bind(data)

          result.errors should contain only formError
      }
    }
  }

  def mandatoryDateField(form: Form[_], key: String, requiredAllKey: String, errorArgs: Seq[String] = Seq.empty): Unit = {

    "fail to bind an empty date" in {

      val result = form.bind(Map.empty[String, String])

      result.errors should contain only FormError(key, requiredAllKey, errorArgs)
    }
  }
}
