/*
 * Copyright 2017 HM Revenue & Customs
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
import models.responsiblepeople.{UKPassportYes, _}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class PersonNonUKPassportControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)
    val dataCacheConnector = mock[DataCacheConnector]

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[DataCacheConnector].to(dataCacheConnector))
      .overrides(bind[AuthConnector].to(self.authConnector))
      .build()

    val controller = app.injector.instanceOf[PersonNonUKPassportController]

    val emptyCache = CacheMap("", Map.empty)
    val mockCacheMap = mock[CacheMap]

    val personName = PersonName("firstname", None, "lastname", None, None)

    val passportNumber = "000000000"
  }

  "PersonNonUKPassportController" when {

    "get is called" must {

      "return OK" when {

        "data is not present" in new Fixture {

          val responsiblePeople = ResponsiblePeople(Some(personName))

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.select("input[name=nonUKPassportNumber]").`val` must be("")
          document.getElementById("nonUKPassport-true").hasAttr("checked") must be(false)
          document.getElementById("nonUKPassport-false").hasAttr("checked") must be(false)

        }

        "data is present" in new Fixture {

          val responsiblePeople = ResponsiblePeople(
            personName = Some(personName),
            nonUKPassport = Some(
              NonUKPassportYes(passportNumber)
            )
          )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
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

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

          val result = controller.get(1)(request)
          status(result) must be(NOT_FOUND)
        }
      }

    }

    "post is called" must {

      "edit is false" must {
        "go to DateOfBirthController" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "nonUKPassport" -> "true",
              "nonUKPassportNumber" -> passportNumber
            )

            val responsiblePeople = ResponsiblePeople()

            when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName = Some(personName))))))

            when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
              .thenReturn(Some(Seq(responsiblePeople)))

            when(controller.dataCacheConnector.fetchAll(any(), any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controller.post(1)(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DateOfBirthController.get(1).url))

          }
      }

      "edit is true" must {
        "go to DetailedAnswersController" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "nonUKPassport" -> "true",
            "nonUKPassportNumber" -> passportNumber
          )

          val responsiblePeople = ResponsiblePeople()

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName = Some(personName))))))

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(Some(Seq(responsiblePeople)))

          when(controller.dataCacheConnector.fetchAll(any(), any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1, true)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1).url))

        }
      }

      "given invalid data" must {
        "respond with BAD_REQUEST" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "nonUKPassport" -> "true",
            "nonUKPassportNumber" -> "abc"
          )

          val responsiblePeople = ResponsiblePeople()

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName = Some(personName))))))

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(Some(Seq(responsiblePeople)))

          when(controller.dataCacheConnector.fetchAll(any(), any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1)(newRequest)
          status(result) must be(BAD_REQUEST)

        }
      }

      "Responsible Person cannot be found with given index" must {
        "respond with NOT_FOUND" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "nonUKPassport" -> "false"
          )

          val responsiblePeople = ResponsiblePeople()

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName = Some(personName))))))

          when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(Some(Seq(responsiblePeople)))

          when(controller.dataCacheConnector.fetchAll(any(), any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(10)(newRequest)
          status(result) must be(NOT_FOUND)

        }
      }

    }

  }

}
