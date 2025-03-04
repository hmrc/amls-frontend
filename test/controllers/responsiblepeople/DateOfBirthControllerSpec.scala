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

import config.ApplicationConfig
import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.responsiblepeople.DateOfBirthFormProvider
import models.responsiblepeople.ResponsiblePerson._
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.AmlsSpec
import views.html.responsiblepeople.DateOfBirthView

import java.time.LocalDate
import scala.concurrent.Future

class DateOfBirthControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture {
    self =>
    val request            = addToken(authRequest)
    val dataCacheConnector = mock[DataCacheConnector]

    val mockApplicationConfig = mock[ApplicationConfig]

    val controller = new DateOfBirthController(
      messagesApi,
      dataCacheConnector,
      SuccessfulAuthAction,
      commonDependencies,
      mockMcc,
      inject[DateOfBirthFormProvider],
      inject[DateOfBirthView],
      errorView
    )

    val emptyCache   = Cache.empty
    val mockCacheMap = mock[Cache]

    val personName = PersonName("firstname", None, "lastname")

  }

  "DateOfBirthController" when {
    "get is called" must {
      "return OK" when {

        "data is not present" in new Fixture {
          val responsiblePeople = ResponsiblePerson(Some(personName))

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.select("input[name=dateOfBirth.day]").`val`   must be("")
          document.select("input[name=dateOfBirth.month]").`val` must be("")
          document.select("input[name=dateOfBirth.year]").`val`  must be("")
        }

        "data is present" in new Fixture {
          val responsiblePeople = ResponsiblePerson(
            personName = Some(personName),
            dateOfBirth = Some(DateOfBirth(LocalDate.of(2001, 12, 2)))
          )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.select("input[name=dateOfBirth.day]").`val`   must be("2")
          document.select("input[name=dateOfBirth.month]").`val` must be("12")
          document.select("input[name=dateOfBirth.year]").`val`  must be("2001")
        }
      }

      "display Not Found" when {
        "a populated ResponsiblePeople model cannot be found" in new Fixture {

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))

          val result = controller.get(1)(request)
          status(result) must be(NOT_FOUND)
        }
      }

    }

    "post is called" must {
      "edit is false" must {
        "go to PersonResidencyTypeController" in new Fixture {

          val newRequest = FakeRequest(POST, routes.DateOfBirthController.post(1).url).withFormUrlEncodedBody(
            "dateOfBirth.day"   -> "1",
            "dateOfBirth.month" -> "12",
            "dateOfBirth.year"  -> "1990"
          )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName = Some(personName))))))

          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1)(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.responsiblepeople.routes.PersonResidentTypeController.get(1).url)
          )

        }
      }

      "edit is true" must {
        "go to DetailedAnswersController" in new Fixture {

          val newRequest = FakeRequest(POST, routes.DateOfBirthController.post(1).url).withFormUrlEncodedBody(
            "dateOfBirth.day"   -> "1",
            "dateOfBirth.month" -> "12",
            "dateOfBirth.year"  -> "1990"
          )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName = Some(personName))))))

          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1, true, Some(flowFromDeclaration))(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1, Some(flowFromDeclaration)).url)
          )

        }
      }

      "given invalid data" must {
        "respond with BAD_REQUEST" in new Fixture {

          val newRequest = FakeRequest(POST, routes.DateOfBirthController.post(1).url).withFormUrlEncodedBody(
            "dateOfBirth.day"   -> "sdfsadgadg",
            "dateOfBirth.month" -> "12",
            "dateOfBirth.year"  -> "1990"
          )

          val responsiblePeople = ResponsiblePerson()

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName = Some(personName))))))

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
            .thenReturn(Some(Seq(responsiblePeople)))

          when(controller.dataCacheConnector.fetchAll(any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1)(newRequest)
          status(result) must be(BAD_REQUEST)

        }
      }

      "Responsible Person cannot be found with given index" must {
        "respond with NOT_FOUND" in new Fixture {

          val newRequest = FakeRequest(POST, routes.DateOfBirthController.post(1).url).withFormUrlEncodedBody(
            "dateOfBirth.day"   -> "1",
            "dateOfBirth.month" -> "12",
            "dateOfBirth.year"  -> "1990"
          )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName = Some(personName))))))

          when(controller.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(10)(newRequest)
          status(result) must be(NOT_FOUND)

        }
      }

    }

  }
}
