/*
 * Copyright 2020 HM Revenue & Customs
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
import models.responsiblepeople.ResponsiblePerson._
import models.responsiblepeople.{DateOfBirth, NonUKPassportYes, PersonName, ResponsiblePerson}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthAction}
import views.html.responsiblepeople.person_non_uk_passport

import scala.concurrent.Future

class PersonNonUKPassportControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture {
    self =>
    val request = addToken(authRequest)
    val dataCacheConnector = mock[DataCacheConnector]

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[DataCacheConnector].to(dataCacheConnector))
      .overrides(bind[AuthAction].to(SuccessfulAuthAction))
      .build()
    lazy val view = app.injector.instanceOf[person_non_uk_passport]

    val mockApplicationConfig = mock[ApplicationConfig]
    val controller = new PersonNonUKPassportController(messagesApi = messagesApi,
      dataCacheConnector,
      SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      person_non_uk_passport = view,
      error = errorView
    )

    val emptyCache = CacheMap("", Map.empty)
    val mockCacheMap = mock[CacheMap]

    val personName = PersonName("firstname", None, "lastname")

    val passportNumber = "000000000"
  }

  "PersonNonUKPassportController" when {

    "get is called" must {

      "return OK" when {

        "data is not present" in new Fixture {

          val responsiblePeople = ResponsiblePerson(Some(personName))

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.select("input[name=nonUKPassportNumber]").`val` must be("")
          document.getElementById("nonUKPassport-true").hasAttr("checked") must be(false)
          document.getElementById("nonUKPassport-false").hasAttr("checked") must be(false)

        }

        "data is present" in new Fixture {

          val responsiblePeople = ResponsiblePerson(
            personName = Some(personName),
            nonUKPassport = Some(
              NonUKPassportYes(passportNumber)
            )
          )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.select("input[name=nonUKPassportNumber]").`val` must be(passportNumber)
          document.getElementById("nonUKPassport-true").hasAttr("checked") must be(true)

        }

      }

      "display Not Found" when {
        "a populated ResponsiblePeople model cannot be found" in new Fixture {

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))

          val result = controller.get(1)(request)
          status(result) must be(NOT_FOUND)
        }
      }

    }

    "post is called" must {
      "edit is false and DOB is defined" must {
        "go to CountryofBirthController" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "nonUKPassport" -> "true",
            "nonUKPassportNumber" -> passportNumber
          )

          val responsiblePeople = ResponsiblePerson(personName = Some(personName), dateOfBirth = Some(DateOfBirth(new LocalDate())))

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
            .thenReturn(Some(Seq(responsiblePeople)))

          when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.dataCacheConnector.save(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(1)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.CountryOfBirthController.get(1).url))

        }
      }

      "edit is false and DOB is not defined" must {
        "go to CountryofBirthController" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "nonUKPassport" -> "true",
            "nonUKPassportNumber" -> passportNumber
          )

          val responsiblePeople = ResponsiblePerson(personName = Some(personName))

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
            .thenReturn(Some(Seq(responsiblePeople)))

          when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.dataCacheConnector.save(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(1)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DateOfBirthController.get(1).url))

        }
      }

      "edit is true" must {
        "go to DetailedAnswersController" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "nonUKPassport" -> "true",
            "nonUKPassportNumber" -> passportNumber
          )

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
            .thenReturn(Some(Seq(ResponsiblePerson(personName = Some(personName),
              dateOfBirth = Some(DateOfBirth(new LocalDate(1990, 1,22)))))))

          when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.dataCacheConnector.save(any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(1, true, Some(flowFromDeclaration))(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1, Some(flowFromDeclaration)).url))

        }
      }

      "given invalid data" must {
        "respond with BAD_REQUEST" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "nonUKPassport" -> "true"
          )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName = Some(personName))))))

          val result = controller.post(1)(newRequest)
          status(result) must be(BAD_REQUEST)

        }
      }

      "Responsible Person cannot be found with given index" must {
        "respond with NOT_FOUND" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "nonUKPassport" -> "false"
          )

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
            .thenReturn(Some(Seq(ResponsiblePerson(personName = Some(personName)))))

          when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post(10)(newRequest)
          status(result) must be(NOT_FOUND)

        }
      }

    }

  }

}
