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

import config.AppConfig
import connectors.DataCacheConnector
import models.responsiblepeople.{KnownBy, PersonName, ResponsiblePerson}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.bind
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}


class KnownByControllerSpec extends AmlsSpec with ScalaFutures {

  trait TestFixture extends AuthorisedFixture with DependencyMocks { self =>
    val request = addToken(self.authRequest)
    val RecordId = 1

    val mockAppConfig = mock[AppConfig]

    val injector = new GuiceInjectorBuilder()
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))
      .overrides(bind[AppConfig].to(mockAppConfig))
      .build()

    lazy val controller = injector.instanceOf[KnownByController]

  }

  "The KnownByController" when {
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
        document.select("input[name=otherName]").`val` must be("")

      }

      "prepopulate the view with data" in new TestFixture {

        val addPerson = PersonName(
          firstName = "first",
          middleName = Some("middle"),
          lastName = "last"
        )

        val otherPerson = KnownBy(
          hasOtherNames = Some(true),
          otherNames = Some("otherName")
        )

        val responsiblePeople = ResponsiblePerson(personName = Some(addPerson), knownBy = Some(otherPerson))

        mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(responsiblePeople)), Some(ResponsiblePerson.key))

        val result = controller.get(RecordId)(request)

        status(result) mustBe OK

        val document = Jsoup.parse(contentAsString(result))

        document.select("input[name=otherNames]").`val` must be("otherName")
      }

    }

    "post is called" must {
      "form is valid" must {
        "go to PersonResidentTypeController" when {
          "edit is false and phase-2-change feature toggle is false" in new TestFixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "hasOtherNames" -> "true",
              "otherNames" -> "otherName"
            )

            when(mockAppConfig.phase2ChangesToggle).thenReturn(false)

            mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(ResponsiblePerson())))
            mockCacheSave[KnownBy]

            val result = controller.post(RecordId)(requestWithParams)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.PersonResidentTypeController.get(RecordId).url))
          }
        }

        "go to DateOfBirthController" when {
          "edit is false and phase-2-change feature toggle is true" in new TestFixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "hasOtherNames" -> "true",
              "otherNames" -> "otherName"
            )

            when(mockAppConfig.phase2ChangesToggle).thenReturn(true)

            mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(ResponsiblePerson())))
            mockCacheSave[KnownBy]

            val result = controller.post(RecordId)(requestWithParams)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.DateOfBirthController.get(RecordId).url))
          }
        }

        "go to DetailedAnswersController" when {
          "edit is true" in new TestFixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "hasOtherNames" -> "true",
              "otherNames" -> "otherName"
            )

            mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(ResponsiblePerson())))
            mockCacheSave[KnownBy]

            val result = controller.post(RecordId, true)(requestWithParams)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(RecordId).url))
          }
        }

        "go to DetailedAnswersController" when {
          "edit is true and does not have other names" in new TestFixture {

          val requestWithParams = request.withFormUrlEncodedBody(
          "hasOtherNames" -> "false"
          )

          mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(ResponsiblePerson())))
          mockCacheSave[KnownBy]

          val result = controller.post(RecordId, true)(requestWithParams)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(RecordId).url))
          }
          }

      }

      "form is invalid" must {
        "return BAD_REQUEST" in new TestFixture {

          val NameMissingInRequest = request.withFormUrlEncodedBody(
            "hasOtherNames" -> "true"
          )

          mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(ResponsiblePerson())))
          mockCacheSave[KnownBy]

          val result = controller.post(RecordId)(NameMissingInRequest)
          status(result) must be(BAD_REQUEST)

        }

      }

      "model cannot be found with given index" must {
        "return NOT_FOUND" in new TestFixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "hasOtherNames" -> "true",
            "otherNames" -> "otherName"
          )

          mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(ResponsiblePerson())))
          mockCacheSave[KnownBy]

          val result = controller.post(2)(requestWithParams)
          status(result) must be(NOT_FOUND)
        }
      }

    }
    
  }

}
