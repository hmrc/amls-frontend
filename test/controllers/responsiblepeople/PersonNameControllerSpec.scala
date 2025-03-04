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

import java.util.UUID
import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.responsiblepeople.PersonNameFormProvider
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import utils.AmlsSpec
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import views.html.responsiblepeople.PersonNameView

import scala.concurrent.Future

class PersonNameControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  val userId                 = s"user-${UUID.randomUUID()}"
  val mockDataCacheConnector = mock[DataCacheConnector]
  val RecordId               = 1

  trait Fixture {
    self =>
    val request              = addToken(authRequest)
    lazy val view            = inject[PersonNameView]
    val personNameController = new PersonNameController(
      dataCacheConnector = mockDataCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      view = view,
      formProvider = inject[PersonNameFormProvider],
      error = errorView
    )
  }

  val emptyCache = Cache.empty

  "PersonNameController" when {

    "get is called" must {

      "display the persons page with blank fields" in new Fixture {

        when(personNameController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))

        val result = personNameController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=firstName]").`val`  must be("")
        document.select("input[name=middleName]").`val` must be("")
        document.select("input[name=lastName]").`val`   must be("")
      }

      "display the persons page with fields populated" in new Fixture {

        val addPerson = PersonName(
          firstName = "first",
          middleName = Some("middle"),
          lastName = "last"
        )

        val responsiblePeople = ResponsiblePerson(Some(addPerson))

        when(personNameController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = personNameController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        document.select("input[name=firstName]").`val`  must be("first")
        document.select("input[name=middleName]").`val` must be("middle")
        document.select("input[name=lastName]").`val`   must be("last")
      }

      "display Not Found" when {
        "ResponsiblePeople model cannot be found" in new Fixture {
          when(personNameController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(None))

          val result = personNameController.get(RecordId)(request)
          status(result) must be(NOT_FOUND)
        }
      }
    }

    "post is called" when {

      "form is valid" must {
        "go to LegalNameController" when {
          "edit is false" in new Fixture {

            val requestWithParams = FakeRequest(POST, routes.PersonNameController.post(1, false).url)
              .withFormUrlEncodedBody(
                "firstName"  -> "first",
                "middleName" -> "middle",
                "lastName"   -> "last"
              )

            when(personNameController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))

            when(personNameController.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
              .thenReturn(Future.successful(emptyCache))

            val result = personNameController.post(RecordId)(requestWithParams)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.LegalNameController.get(RecordId).url))
          }
        }
        "go to DetailedAnswersController" when {
          "edit is true" in new Fixture {

            val requestWithParams = FakeRequest(POST, routes.PersonNameController.post(1, false).url)
              .withFormUrlEncodedBody(
                "firstName"  -> "first",
                "middleName" -> "middle",
                "lastName"   -> "last"
              )

            when(personNameController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))

            when(personNameController.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
              .thenReturn(Future.successful(emptyCache))

            val result = personNameController.post(RecordId, true)(requestWithParams)
            status(result) must be(SEE_OTHER)
          }
        }
      }

      "form is invalid" must {
        "return BAD_REQUEST" in new Fixture {

          val firstNameMissingInRequest = FakeRequest(POST, routes.PersonNameController.post(1, false).url)
            .withFormUrlEncodedBody(
              "lastName" -> "last"
            )

          when(personNameController.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = personNameController.post(RecordId)(firstNameMissingInRequest)
          status(result) must be(BAD_REQUEST)

        }

      }

      "model cannot be found with given index" must {
        "return NOT_FOUND" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.PersonNameController.post(1, false).url)
            .withFormUrlEncodedBody(
              "firstName" -> "first",
              "lastName"  -> "last"
            )

          when(personNameController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))

          when(personNameController.dataCacheConnector.save[PersonName](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = personNameController.post(2)(requestWithParams)
          status(result) must be(NOT_FOUND)
        }
      }
    }
  }
}
