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
import forms.responsiblepeople.LegalNameFormProvider
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.scalatest.concurrent.ScalaFutures
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import utils._
import views.html.responsiblepeople.LegalNameView

class LegalNameControllerSpec extends AmlsSpec with ScalaFutures with Injecting {

  trait TestFixture extends AuthorisedFixture with DependencyMocks { self =>
    val request         = addToken(self.authRequest)
    val RecordId        = 1
    lazy val view       = inject[LegalNameView]
    lazy val controller = new LegalNameController(
      mockCacheConnector,
      SuccessfulAuthAction,
      commonDependencies,
      cc = mockMcc,
      formProvider = inject[LegalNameFormProvider],
      view = view,
      error = errorView
    )
  }

  "The LegalNameController" when {
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
      }

      "prepopulate the view with data" in new TestFixture {

        val addPerson = PersonName(
          firstName = "first",
          middleName = Some("middle"),
          lastName = "last"
        )

        val previousPerson = PreviousName(
          hasPreviousName = Some(true),
          None,
          None,
          None
        )

        val responsiblePeople = ResponsiblePerson(personName = Some(addPerson), legalName = Some(previousPerson))

        mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(responsiblePeople)), Some(ResponsiblePerson.key))

        val result = controller.get(RecordId)(request)

        status(result) mustBe OK

        val document = Jsoup.parse(contentAsString(result))

        document.select("input[name=hasPreviousName]").`val` must be("true")
      }
    }

    "post is called" must {
      "form is valid" must {
        "go to LegalNameChangeDateController" when {
          "edit is false" in new TestFixture {

            val requestWithParams = FakeRequest(POST, routes.LegalNameController.post(1).url)
              .withFormUrlEncodedBody(
                "hasPreviousName" -> "true"
              )

            mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(ResponsiblePerson())))
            mockCacheSave[PreviousName]

            val result = controller.post(RecordId)(requestWithParams)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.LegalNameInputController.get(RecordId).url))
          }
        }

        "go to DetailedAnswersController" when {
          "edit is true" in new TestFixture {

            val requestWithParams = FakeRequest(POST, routes.LegalNameController.post(1).url)
              .withFormUrlEncodedBody(
                "hasPreviousName" -> "true"
              )

            mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(ResponsiblePerson())))
            mockCacheSave[PreviousName]

            val result = controller.post(RecordId, true)(requestWithParams)
            status(result) must be(SEE_OTHER)
          }

          "edit is true and does not have previous names" in new TestFixture {

            val requestWithParams = FakeRequest(POST, routes.LegalNameController.post(1).url)
              .withFormUrlEncodedBody(
                "hasPreviousName" -> "false"
              )

            mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(ResponsiblePerson())))
            mockCacheSave[PreviousName]

            val result = controller.post(RecordId, true)(requestWithParams)
            status(result) must be(SEE_OTHER)
          }
        }
      }

      "form is invalid" must {
        "return BAD_REQUEST" in new TestFixture {

          val notValidData = FakeRequest(POST, routes.LegalNameController.post(1).url)
            .withFormUrlEncodedBody(
              "hasPreviousName" -> "fail"
            )

          mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(ResponsiblePerson())))
          mockCacheSave[PreviousName]

          val result = controller.post(RecordId)(notValidData)
          status(result) must be(BAD_REQUEST)

        }
      }

      "form is invalid" when {
        "hasPreviousName is true" must {
          "redirect to LegalNameInputController" in new TestFixture {

            val requestWithHasPreviousNameTrue = FakeRequest(POST, routes.LegalNameController.post(1).url)
              .withFormUrlEncodedBody(
                "hasPreviousName" -> "true"
              )

            mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(ResponsiblePerson())))
            mockCacheSave[PreviousName]

            val result = controller.post(RecordId)(requestWithHasPreviousNameTrue)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.LegalNameInputController.get(RecordId).url))
          }
        }
      }

      "model cannot be found with given index" must {
        "return NOT_FOUND" in new TestFixture {

          val requestWithParams = FakeRequest(POST, routes.LegalNameController.post(1).url)
            .withFormUrlEncodedBody(
              "hasPreviousName" -> "true"
            )

          mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(ResponsiblePerson())))
          mockCacheSave[PreviousName]

          val result = controller.post(2)(requestWithParams)
          status(result) must be(NOT_FOUND)
        }
      }

    }

  }

}
