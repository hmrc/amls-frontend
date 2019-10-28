/*
 * Copyright 2019 HM Revenue & Customs
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

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.bind
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.test.Helpers._
import utils._


class LegalNameInputControllerSpec extends AmlsSpec with ScalaFutures {

  trait TestFixture extends AuthorisedFixture with DependencyMocks { self =>
    val request = addToken(self.authRequest)
    val RecordId = 1

    val injector = new GuiceInjectorBuilder()
      .overrides(bind[AuthAction].to(SuccessfulAuthAction))
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))
      .build()

    lazy val controller = injector.instanceOf[LegalNameInputController]

  }

  "The LegalNameInputController" when {
    "get is called" must {
      "load the page" in new TestFixture {
        val addPerson = PersonName(
          firstName = "first",
          middleName = Some("middle"),
          lastName = "last"
        )

        val previousPerson = PreviousName(
          hasPreviousName = Some(true),
          firstName = Some("first"),
          middleName = Some("middle"),
          lastName = Some("last")
        )

        val responsiblePeople = ResponsiblePerson(personName = Some(addPerson), legalName = Some(previousPerson))


        mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(responsiblePeople)), Some(ResponsiblePerson.key))


        val result = controller.get(RecordId)(request)

        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=firstName]").`val` must be("first")
        document.select("input[name=middleName]").`val` must be("middle")
        document.select("input[name=lastName]").`val` must be("last")
      }

      "prepopulate the view with data" in new TestFixture {

        val addPerson = PersonName(
          firstName = "first",
          middleName = Some("middle"),
          lastName = "last"
        )

        val previousPerson = PreviousName(
          hasPreviousName = Some(true),
          firstName = Some("Matty"),
          middleName = Some("James"),
          lastName = Some("Harris")
        )

        val responsiblePeople = ResponsiblePerson(personName = Some(addPerson), legalName = Some(previousPerson))

        mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(responsiblePeople)), Some(ResponsiblePerson.key))


        val result = controller.get(RecordId)(request)

        status(result) mustBe OK

        val document = Jsoup.parse(contentAsString(result))

        document.select("input[name=hasPreviousName]").`val` must be("true")
        document.select("input[name=firstName]").`val` must be("Matty")
        document.select("input[name=middleName]").`val` must be("James")
        document.select("input[name=lastName]").`val` must be("Harris")
      }

    }

    "post is called" must {
      "form is valid" must {
        "go to LegalNameChangeDateController" when {
          "edit is false" in new TestFixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "hasPreviousName" -> "true",
              "firstName" -> "first",
              "middleName" -> "middle",
              "lastName" -> "last"
            )

            mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(ResponsiblePerson())))
            mockCacheSave[PreviousName]

            val result = controller.post(RecordId)(requestWithParams)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.LegalNameChangeDateController.get(RecordId).url))
          }
        }

        "go to DetailedAnswersController" when {
          "edit is true" in new TestFixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "hasPreviousName" -> "true",
              "firstName" -> "first",
              "middleName" -> "middle",
              "lastName" -> "last"
            )

            mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(ResponsiblePerson())))
            mockCacheSave[PreviousName]

            val result = controller.post(RecordId, true)(requestWithParams)
            status(result) must be(SEE_OTHER)
          }

          "edit is true and does not have previous names" in new TestFixture {

            val requestWithParams = request.withFormUrlEncodedBody(
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

          val NameMissingInRequest = request.withFormUrlEncodedBody(
            "hasPreviousName" -> "fail"
          )

          mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(ResponsiblePerson())))
          mockCacheSave[PreviousName]

          val result = controller.post(RecordId)(NameMissingInRequest)
          status(result) must be(BAD_REQUEST)

        }

      }

      "model cannot be found with given index" must {
        "return NOT_FOUND" in new TestFixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "hasPreviousName" -> "true",
            "firstName" -> "first",
            "lastName" -> "last"
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
