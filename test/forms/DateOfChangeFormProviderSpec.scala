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

package forms

import forms.behaviours.DateBehaviours
import models.DateOfChange
import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import play.api.test.{FakeRequest, Helpers}

import java.time.LocalDate
import scala.collection.mutable

class DateOfChangeFormProviderSpec extends DateBehaviours {

  val formProvider: DateOfChangeFormProvider = new DateOfChangeFormProvider()
  val form: Form[DateOfChange]               = formProvider()

  val messages: Messages = Helpers.stubMessagesApi().preferred(FakeRequest())

  val formField = "dateOfChange"

  val minDate: LocalDate = DateOfChangeFormProvider.minDate
  def maxDate: LocalDate = DateOfChangeFormProvider.maxDate

  "Date Of Change form" must {

    "bind valid data" in {

      forAll(datesBetween(minDate, maxDate)) { date =>
        val data = Map(
          s"$formField.day"   -> date.getDayOfMonth.toString,
          s"$formField.month" -> date.getMonthValue.toString,
          s"$formField.year"  -> date.getYear.toString
        )

        val result = form.bind(data)

        result.value.value shouldEqual DateOfChange(date)
      }
    }

    "fail to bind" when {

      val fields    = List("day", "month", "year")
      val fieldsTwo = List(
        ("day", "month"),
        ("day", "year"),
        ("month", "year")
      )

      fields foreach { field =>
        s"$field is blank" in {

          forAll(datesBetween(minDate, maxDate)) { date =>
            val data = mutable.Map(
              s"$formField.day"   -> date.getDayOfMonth.toString,
              s"$formField.month" -> date.getMonthValue.toString,
              s"$formField.year"  -> date.getYear.toString
            )

            data(s"$formField.$field") = ""

            val result = form.bind(data.toMap)

            result.errors.headOption shouldEqual Some(
              FormError(s"$formField.$field", messages("error.required.dateofchange.one"), Seq(field))
            )
          }
        }

        s"$field is in the incorrect format" in {
          val data = mutable.Map(
            s"$formField.day"   -> "11",
            s"$formField.month" -> "11",
            s"$formField.year"  -> "2000"
          )

          data(s"$formField.$field") = "x"

          val result = form.bind(data.toMap)

          result.errors.headOption shouldEqual Some(
            FormError(s"$formField.$field", messages("error.invalid.dateofchange.one"), Seq(field))
          )
        }
      }

      fieldsTwo foreach { fields =>
        s"${fields._1} and ${fields._2} are blank" in {

          forAll(datesBetween(minDate, maxDate)) { date =>
            val data = mutable.Map(
              s"$formField.day"   -> date.getDayOfMonth.toString,
              s"$formField.month" -> date.getMonthValue.toString,
              s"$formField.year"  -> date.getYear.toString
            )

            data(s"$formField.${fields._1}") = ""
            data(s"$formField.${fields._2}") = ""

            val result = form.bind(data.toMap)

            result.errors.headOption shouldEqual Some(
              FormError(
                s"$formField.${fields._1}",
                messages("error.required.dateofchange.two"),
                Seq(fields._1, fields._2)
              )
            )
          }
        }

        s"${fields._1} and ${fields._2} are in the incorrect format" in {
          val data = mutable.Map(
            s"$formField.day"   -> "11",
            s"$formField.month" -> "11",
            s"$formField.year"  -> "2000"
          )

          data(s"$formField.${fields._1}") = "x"
          data(s"$formField.${fields._2}") = "x"

          val result = form.bind(data.toMap)

          result.errors.headOption shouldEqual Some(
            FormError(
              s"$formField.${fields._1}",
              messages("error.invalid.dateofchange.multiple"),
              Seq(fields._1, fields._2)
            )
          )
        }
      }
    }

    behave like mandatoryDateField(form, formField, "error.required.dateofchange.all")

    behave like dateFieldWithMin(
      form,
      formField,
      minDate,
      FormError(formField, "error.invalid.dateofchange.before.1900")
    )

    behave like dateFieldWithMax(form, formField, maxDate, FormError(formField, "error.invalid.dateofchange.future"))

    behave like realDateField(form, formField, "error.required.dateofchange.not.real")
  }
}
