/*
 * Copyright 2018 HM Revenue & Customs
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
import models.responsiblepeople.ResponsiblePeople._
import models.responsiblepeople._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito.{verify, when}
import org.scalatest.mock.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, AmlsSpec}
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

class PersonUKPassportControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)
    val dataCacheConnector = mock[DataCacheConnector]

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[DataCacheConnector].to(dataCacheConnector))
      .overrides(bind[AuthConnector].to(self.authConnector))
      .build()

    val controller = app.injector.instanceOf[PersonUKPassportController]

    val emptyCache = CacheMap("", Map.empty)
    val mockCacheMap = mock[CacheMap]

    val personName = PersonName("firstname", None, "lastname")

    val ukPassportNumber = "000000000"
  }

  "PersonUKPassportController" when {

    "get is called" must {

      "return OK" when {

        "data is not present" in new Fixture {

          val responsiblePeople = ResponsiblePeople(Some(personName))

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.select("input[name=ukPassportNumber]").`val` must be("")
          document.getElementById("ukPassport-true").hasAttr("checked") must be(false)
          document.getElementById("ukPassport-false").hasAttr("checked") must be(false)

        }

        "data is present" in new Fixture {

          val responsiblePeople = ResponsiblePeople(
            personName = Some(personName),
            ukPassport = Some(
              UKPassportYes("000000000")
            )
          )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.select("input[name=ukPassportNumber]").`val` must be("000000000")
          document.getElementById("ukPassport-true").hasAttr("checked") must be(true)

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

    "post is called" when {

      "edit is false" must {
        "go to DateOfBirthController" when {
          "uk passport number is provided" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "ukPassport" -> "true",
              "ukPassportNumber" -> ukPassportNumber
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
        "go to PersonNonUKPassportController" when {
          "no uk passport" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "ukPassport" -> "false"
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
            redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.PersonNonUKPassportController.get(1).url))
          }

          "existing data is present" in new Fixture {
            val newRequest = request.withFormUrlEncodedBody(
              "ukPassport" -> "false"
            )

            val responsiblePeople = ResponsiblePeople(
              ukPassport = Some(UKPassportNo)
            )

            when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName = Some(personName))))))

            when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
              .thenReturn(Some(Seq(responsiblePeople)))

            when(controller.dataCacheConnector.fetchAll(any(), any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controller.post(1, false)(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.PersonNonUKPassportController.get(1, false).url))
          }
        }
      }

      "edit is true" must {
        "go to PersonNonUKPassportController" when {
          "data is changed from uk passport to non uk passport" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "ukPassport" -> "false"
            )

            val responsiblePeople = ResponsiblePeople(
              ukPassport = Some(UKPassportYes(ukPassportNumber))
            )

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
            redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.PersonNonUKPassportController.get(1, true).url))
          }
        }
        "goes to DateOfBirthController" when {
          "uk passport and DateOfBirth data does not exist" in new Fixture {
            val newRequest = request.withFormUrlEncodedBody(
              "ukPassport" -> "true",
              "ukPassportNumber" -> ukPassportNumber
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
            redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DateOfBirthController.get(1, true).url))

          }
        }
        "go to DetailedAnswersController" when {
          "uk passport" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "ukPassport" -> "true",
              "ukPassportNumber" -> ukPassportNumber
            )

            val responsiblePeople = ResponsiblePeople(
              ukPassport = Some(UKPassportNo),
              dateOfBirth = Some(DateOfBirth(new LocalDate(2001,12,1)))
            )

            when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName = Some(personName))))))

            when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
              .thenReturn(Some(Seq(responsiblePeople)))

            when(controller.dataCacheConnector.fetchAll(any(), any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controller.post(1, true, Some(flowFromDeclaration))(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1, true, Some(flowFromDeclaration)).url))

          }

          "non uk passport has not been changed" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "ukPassport" -> "false"
            )

            val responsiblePeople = ResponsiblePeople(
              ukPassport = Some(UKPassportNo)
            )

            when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName = Some(personName))))))

            when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
              .thenReturn(Some(Seq(responsiblePeople)))

            when(controller.dataCacheConnector.fetchAll(any(), any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controller.post(1, true, Some(flowFromDeclaration))(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1, true, Some(flowFromDeclaration)).url))

          }
        }
      }

      "given invalid data" must {
        "respond with BAD_REQUEST" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "ukPassport" -> "true",
            "ukPassportNumber" -> "abc"
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
            "ukPassport" -> "false"
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

  it must {
    "remove non uk passport data" when {
      "data is changed to uk passport" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "ukPassport" -> "true",
          "ukPassportNumber" -> ukPassportNumber
        )

        val responsiblePeople = ResponsiblePeople(
          ukPassport = Some(UKPassportNo),
          nonUKPassport = Some(NoPassport)
        )

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(personName = Some(personName))))))

        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
          .thenReturn(Some(Seq(responsiblePeople)))

        when(controller.dataCacheConnector.fetchAll(any(), any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val result = controller.post(1, true)(newRequest)
        status(result) must be(SEE_OTHER)

        verify(controller.dataCacheConnector)
          .save[Seq[ResponsiblePeople]](any(), meq(Seq(responsiblePeople.copy(
          ukPassport = Some(UKPassportYes(ukPassportNumber)),
          nonUKPassport = None,
          hasChanged = true
        ))))(any(), any(), any())

      }
    }
  }
}
