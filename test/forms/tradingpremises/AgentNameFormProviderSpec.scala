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

package forms.tradingpremises

import forms.behaviours.{DateBehaviours, StringFieldBehaviours}
import forms.mappings.Constraints
import models.tradingpremises.AgentName
import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import play.api.test.{FakeRequest, Helpers}

import java.time.LocalDate
import scala.collection.mutable

class AgentNameFormProviderSpec extends DateBehaviours with StringFieldBehaviours with Constraints {

  val formProvider: AgentNameFormProvider = new AgentNameFormProvider()
  val form: Form[AgentName]               = formProvider()

  val messages: Messages = Helpers.stubMessagesApi().preferred(FakeRequest())

  val nameFieldName = "agentName"
  val dateFieldName = "agentDateOfBirth"

  val maxDate: LocalDate = LocalDate.now()
  val minDate: LocalDate = LocalDate.of(1900, 1, 1)

  "AgentNameFormProvider" when {

    s"$nameFieldName is validated" must {

      behave like fieldThatBindsValidData(
        form,
        nameFieldName,
        numStringOfLength(formProvider.length)
      )

      behave like mandatoryField(
        form,
        nameFieldName,
        FormError(nameFieldName, "error.required.tp.agent.name")
      )

      behave like fieldWithMaxLength(
        form,
        nameFieldName,
        formProvider.length,
        FormError(nameFieldName, "error.length.tp.agent.name", Seq(formProvider.length))
      )

      "fail to bind strings with special characters" in {

        forAll(alphaStringsShorterThan(formProvider.length).suchThat(_.nonEmpty), invalidCharForNames) {
          (str, invalidStr) =>
            val result = form.bind(Map(nameFieldName -> (str + invalidStr)))

            result.value                      shouldBe None
            result.error(nameFieldName).value shouldBe FormError(
              nameFieldName,
              "error.char.tp.agent.name",
              Seq(basicPunctuationRegex)
            )
        }
      }
    }

    s"$dateFieldName is validated" must {

      val agentName = "John Doe"

      "bind valid data" in {

        forAll(datesBetween(minDate, maxDate)) { date =>
          val data = Map(
            nameFieldName           -> agentName,
            s"$dateFieldName.day"   -> date.getDayOfMonth.toString,
            s"$dateFieldName.month" -> date.getMonthValue.toString,
            s"$dateFieldName.year"  -> date.getYear.toString
          )

          val result = form.bind(data)

          result.value.value shouldEqual AgentName(agentName, agentDateOfBirth = Some(date))
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
                nameFieldName           -> agentName,
                s"$dateFieldName.day"   -> date.getDayOfMonth.toString,
                s"$dateFieldName.month" -> date.getMonthValue.toString,
                s"$dateFieldName.year"  -> date.getYear.toString
              )

              data(s"$dateFieldName.$field") = ""

              val result = form.bind(data.toMap)

              result.errors.headOption shouldEqual Some(
                FormError(s"$dateFieldName.$field", messages("error.required.tp.agent.date.one"), Seq(field))
              )
            }
          }

          s"$field is in the incorrect format" in {
            val data = mutable.Map(
              nameFieldName           -> agentName,
              s"$dateFieldName.day"   -> "11",
              s"$dateFieldName.month" -> "11",
              s"$dateFieldName.year"  -> "2000"
            )

            data(s"$dateFieldName.$field") = "x"

            val result = form.bind(data.toMap)

            result.errors.headOption shouldEqual Some(
              FormError(s"$dateFieldName.$field", messages("error.invalid.tp.agent.date.one"), Seq(field))
            )
          }
        }

        fieldsTwo foreach { fields =>
          s"${fields._1} and ${fields._2} are blank" in {

            forAll(datesBetween(minDate, maxDate)) { date =>
              val data = mutable.Map(
                nameFieldName           -> agentName,
                s"$dateFieldName.day"   -> date.getDayOfMonth.toString,
                s"$dateFieldName.month" -> date.getMonthValue.toString,
                s"$dateFieldName.year"  -> date.getYear.toString
              )

              data(s"$dateFieldName.${fields._1}") = ""
              data(s"$dateFieldName.${fields._2}") = ""

              val result = form.bind(data.toMap)

              result.errors.headOption shouldEqual Some(
                FormError(
                  s"$dateFieldName.${fields._1}",
                  messages("error.required.tp.agent.date.two"),
                  Seq(fields._1, fields._2)
                )
              )
            }
          }

          s"${fields._1} and ${fields._2} are in the incorrect format" in {
            val data = mutable.Map(
              nameFieldName           -> agentName,
              s"$dateFieldName.day"   -> "11",
              s"$dateFieldName.month" -> "11",
              s"$dateFieldName.year"  -> "2000"
            )

            data(s"$dateFieldName.${fields._1}") = "x"
            data(s"$dateFieldName.${fields._2}") = "x"

            val result = form.bind(data.toMap)

            result.errors.headOption shouldEqual Some(
              FormError(
                s"$dateFieldName.${fields._1}",
                messages("error.invalid.tp.agent.date.multiple"),
                Seq(fields._1, fields._2)
              )
            )
          }
        }

        "date fields are empty" in {

          val result = form.bind(
            Map(
              nameFieldName -> agentName
            )
          )

          result.errors should contain only FormError(dateFieldName, "error.required.tp.agent.date.all")
        }

        s"date is greater than ${maxDate.getDayOfMonth}/${maxDate.getMonthValue}/${maxDate.getYear}" in {

          forAll(datesBetween(maxDate.plusDays(1), maxDate.plusYears(10))) { date =>
            val data = Map(
              nameFieldName           -> agentName,
              s"$dateFieldName.day"   -> date.getDayOfMonth.toString,
              s"$dateFieldName.month" -> date.getMonthValue.toString,
              s"$dateFieldName.year"  -> date.getYear.toString
            )

            val result = form.bind(data)

            result.errors should contain only FormError(dateFieldName, "error.invalid.agent.date.future")
          }
        }
      }
    }
  }
}
