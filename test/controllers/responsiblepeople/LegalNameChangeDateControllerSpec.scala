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

package controllers.responsiblepeople

import controllers.actions.SuccessfulAuthAction
import forms.responsiblepeople.LegalNameChangeDateFormProvider
import models.responsiblepeople.{LegalNameChangeDate, PersonName, ResponsiblePerson}
import org.jsoup.Jsoup
import org.scalatest.concurrent.ScalaFutures
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}
import views.html.responsiblepeople.LegalNameChangeDateView

import java.time.LocalDate

class LegalNameChangeDateControllerSpec extends AmlsSpec with ScalaFutures with Injecting {

  trait TestFixture extends AuthorisedFixture with DependencyMocks { self =>
    val request         = addToken(self.authRequest)
    val RecordId        = 1
    lazy val view       = inject[LegalNameChangeDateView]
    lazy val controller = new LegalNameChangeDateController(
      mockCacheConnector,
      SuccessfulAuthAction,
      commonDependencies,
      cc = mockMcc,
      formProvider = inject[LegalNameChangeDateFormProvider],
      view = view,
      error = errorView
    )

    val personName = PersonName("firstname", None, "lastname")
  }

  "The LegalNameChangeDateController" when {
    "get is called" must {
      "load the page" in new TestFixture {
        val addPerson = PersonName(
          firstName = "first",
          middleName = Some("middle"),
          lastName = "last"
        )

        val responsiblePeople = ResponsiblePerson(personName = Some(addPerson))

        mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(responsiblePeople)), Some(ResponsiblePerson.key))

        val result = controller.get(RecordId)(request)

        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=date]").`val` must be("")

      }

      "prepopulate the view with data" in new TestFixture {

        val responsiblePeople = ResponsiblePerson(
          personName = Some(personName),
          legalNameChangeDate = Some(LocalDate.of(2001, 10, 14))
        )

        mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(responsiblePeople)), Some(ResponsiblePerson.key))

        val result = controller.get(RecordId)(request)

        status(result) mustBe OK

        val document = Jsoup.parse(contentAsString(result))

        document.select("input[name=date.day]").`val`   must be("14")
        document.select("input[name=date.month]").`val` must be("10")
        document.select("input[name=date.year]").`val`  must be("2001")
      }

    }

    "post is called" must {
      "form is valid" must {
        "go to KnownByController" when {
          "edit is false" in new TestFixture {

            val newRequest = FakeRequest(POST, routes.LegalNameChangeDateController.post(1).url)
              .withFormUrlEncodedBody(
                "date.day"   -> "1",
                "date.month" -> "12",
                "date.year"  -> "1990"
              )

            mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(ResponsiblePerson())))
            mockCacheSave[LegalNameChangeDate]

            val result = controller.post(RecordId)(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.KnownByController.get(RecordId).url))
          }
        }

        "go to DetailedAnswersController" when {
          "edit is true" in new TestFixture {

            val newRequest = FakeRequest(POST, routes.LegalNameChangeDateController.post(1).url)
              .withFormUrlEncodedBody(
                "date.day"   -> "1",
                "date.month" -> "12",
                "date.year"  -> "1990"
              )

            mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(ResponsiblePerson())))
            mockCacheSave[LegalNameChangeDate]

            val result = controller.post(RecordId, true)(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(RecordId).url))
          }
        }
      }

      "form is invalid" must {
        "return BAD_REQUEST" in new TestFixture {

          val NameMissingInRequest = FakeRequest(POST, routes.LegalNameChangeDateController.post(1).url)
            .withFormUrlEncodedBody(
              "date.day" -> "1"
            )

          mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(ResponsiblePerson())))
          mockCacheSave[LegalNameChangeDate]

          val result = controller.post(RecordId)(NameMissingInRequest)
          status(result) must be(BAD_REQUEST)

        }

      }

      "model cannot be found with given index" must {
        "return NOT_FOUND" in new TestFixture {

          val newRequest = FakeRequest(POST, routes.LegalNameChangeDateController.post(1).url)
            .withFormUrlEncodedBody(
              "date.day"   -> "1",
              "date.month" -> "12",
              "date.year"  -> "1990"
            )

          mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(ResponsiblePerson())))
          mockCacheSave[LegalNameChangeDate]

          val result = controller.post(2)(newRequest)
          status(result) must be(NOT_FOUND)
        }
      }

    }

  }

}
