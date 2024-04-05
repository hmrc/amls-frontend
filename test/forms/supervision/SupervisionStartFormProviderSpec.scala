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

package forms.supervision

import forms.behaviours.JodaDateBehaviours
import models.supervision.SupervisionStart
import org.joda.time.LocalDate
import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import play.api.test.{FakeRequest, Helpers}

import scala.collection.mutable

class SupervisionStartFormProviderSpec extends JodaDateBehaviours {

  val formProvider: SupervisionStartFormProvider = new SupervisionStartFormProvider
  val form: Form[SupervisionStart] = formProvider()

  val messages: Messages = Helpers.stubMessagesApi().preferred(FakeRequest())

  val formField = "startDate"

  val minDate: LocalDate = SupervisionStartFormProvider.minDate
  def maxDate: LocalDate = SupervisionStartFormProvider.maxDate

  "Supervision Start Date form" must {

    "bind valid data" in {

      forAll(jodaDatesBetween(minDate, maxDate)) { date =>

        val data = Map(
          s"$formField.day" -> date.getDayOfMonth.toString,
          s"$formField.month" -> date.getMonthOfYear.toString,
          s"$formField.year" -> date.getYear.toString
        )

        val result = form.bind(data)

        result.value.value shouldEqual SupervisionStart(date)
      }
    }

    "fail to bind" when {

      val fields = List("day", "month", "year")
      val fieldsTwo = List(
        ("day", "month"),
        ("day", "year"),
        ("month", "year")
      )

      fields foreach { field =>

        s"$field is blank" in {

          forAll(jodaDatesBetween(minDate, maxDate)) { date =>

            val data = mutable.Map(
              s"$formField.day" -> date.getDayOfMonth.toString,
              s"$formField.month" -> date.getMonthOfYear.toString,
              s"$formField.year" -> date.getYear.toString
            )

            data(s"$formField.$field") = ""

            val result = form.bind(data.toMap)

            result.errors.headOption shouldEqual Some(
              FormError(formField, messages("error.supervision.start.required.date.one"), Seq(field))
            )
          }
        }
      }

      fieldsTwo foreach { fields =>

        s"${fields._1} and ${fields._2} are blank" in {

          forAll(jodaDatesBetween(minDate, maxDate)) { date =>

            val data = mutable.Map(
              s"$formField.day" -> date.getDayOfMonth.toString,
              s"$formField.month" -> date.getMonthOfYear.toString,
              s"$formField.year" -> date.getYear.toString
            )

            data(s"$formField.${fields._1}") = ""
            data(s"$formField.${fields._2}") = ""

            val result = form.bind(data.toMap)

            result.errors.headOption shouldEqual Some(
              FormError(formField, messages("error.supervision.start.required.date.two"), Seq(fields._1, fields._2))
            )
          }
        }
      }
    }

    behave like mandatoryDateField(form, formField, "error.supervision.start.required.date.all")

    behave like dateFieldWithMin(form, formField, minDate, FormError(formField, "error.supervision.start.invalid.date.after.1900"))

    behave like dateFieldWithMax(form, formField, maxDate, FormError(formField, "error.supervision.start.invalid.date.future"))
  }
}