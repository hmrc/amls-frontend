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
import forms.responsiblepeople.PersonUKPassportFormProvider
import models.responsiblepeople.ResponsiblePerson._
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.AmlsSpec
import views.html.responsiblepeople.PersonUKPassportView

import java.time.LocalDate
import scala.concurrent.Future

class PersonUKPassportControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture {
    self =>
    val request            = addToken(authRequest)
    val dataCacheConnector = mock[DataCacheConnector]

    val mockApplicationConfig = mock[ApplicationConfig]

    val controller = new PersonUKPassportController(
      messagesApi,
      dataCacheConnector,
      SuccessfulAuthAction,
      commonDependencies,
      mockMcc,
      inject[PersonUKPassportFormProvider],
      inject[PersonUKPassportView],
      errorView
    )

    val emptyCache   = Cache.empty
    val mockCacheMap = mock[Cache]

    val personName = PersonName("firstname", None, "lastname")

    val ukPassportNumber = "000000000"
  }

  "PersonUKPassportController" when {

    "get is called" must {

      "return OK" when {

        "data is not present" in new Fixture {

          val responsiblePeople = ResponsiblePerson(Some(personName))

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.select("input[name=ukPassportNumber]").`val`          must be("")
          document.getElementById("ukPassport-true").hasAttr("checked")  must be(false)
          document.getElementById("ukPassport-false").hasAttr("checked") must be(false)

        }

        "data is present" in new Fixture {

          val responsiblePeople = ResponsiblePerson(
            personName = Some(personName),
            ukPassport = Some(
              UKPassportYes("000000000")
            )
          )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.select("input[name=ukPassportNumber]").`val`         must be("000000000")
          document.getElementById("ukPassport-true").hasAttr("checked") must be(true)

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

    "post is called" when {

      "edit is false" must {
        "go to CountryOfBirthController" when {
          "uk passport number is provided" in new Fixture {

            val newRequest = FakeRequest(POST, routes.PersonUKPassportController.post(1).url)
              .withFormUrlEncodedBody(
                "ukPassport"       -> "true",
                "ukPassportNumber" -> ukPassportNumber
              )

            val responsiblePeople = ResponsiblePerson()

            when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName = Some(personName))))))

            when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
              .thenReturn(Some(Seq(responsiblePeople)))

            when(controller.dataCacheConnector.fetchAll(any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(controller.dataCacheConnector.save(any(), any(), any())(any()))
              .thenReturn(Future.successful(mockCacheMap))

            val result = controller.post(1)(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(
              Some(controllers.responsiblepeople.routes.CountryOfBirthController.get(1).url)
            )

          }
        }
        "go to PersonNonUKPassportController" when {
          "no uk passport" in new Fixture {

            val newRequest = FakeRequest(POST, routes.PersonUKPassportController.post(1).url)
              .withFormUrlEncodedBody(
                "ukPassport" -> "false"
              )

            val responsiblePeople = ResponsiblePerson()

            when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName = Some(personName))))))

            when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
              .thenReturn(Some(Seq(responsiblePeople)))

            when(controller.dataCacheConnector.fetchAll(any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(controller.dataCacheConnector.save(any(), any(), any())(any()))
              .thenReturn(Future.successful(mockCacheMap))

            val result = controller.post(1)(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(
              Some(controllers.responsiblepeople.routes.PersonNonUKPassportController.get(1).url)
            )
          }

          "existing data is present" in new Fixture {
            val newRequest = FakeRequest(POST, routes.PersonUKPassportController.post(1).url)
              .withFormUrlEncodedBody(
                "ukPassport" -> "false"
              )

            val responsiblePeople = ResponsiblePerson(
              ukPassport = Some(UKPassportNo)
            )

            when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName = Some(personName))))))

            when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
              .thenReturn(Some(Seq(responsiblePeople)))

            when(controller.dataCacheConnector.fetchAll(any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(controller.dataCacheConnector.save(any(), any(), any())(any()))
              .thenReturn(Future.successful(mockCacheMap))

            val result = controller.post(1, false)(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(
              Some(controllers.responsiblepeople.routes.PersonNonUKPassportController.get(1, false).url)
            )
          }
        }
      }

      "edit is true" must {
        "go to PersonNonUKPassportController" when {
          "data is changed from uk passport to non uk passport" in new Fixture {

            val newRequest = FakeRequest(POST, routes.PersonUKPassportController.post(1).url)
              .withFormUrlEncodedBody(
                "ukPassport" -> "false"
              )

            val responsiblePeople = ResponsiblePerson(
              ukPassport = Some(UKPassportYes(ukPassportNumber))
            )

            when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName = Some(personName))))))

            when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
              .thenReturn(Some(Seq(responsiblePeople)))

            when(controller.dataCacheConnector.fetchAll(any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(controller.dataCacheConnector.save(any(), any(), any())(any()))
              .thenReturn(Future.successful(mockCacheMap))

            val result = controller.post(1, true)(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(
              Some(controllers.responsiblepeople.routes.PersonNonUKPassportController.get(1, true).url)
            )
          }
        }

        "go to DetailedAnswersController" when {
          "uk passport" in new Fixture {

            val newRequest = FakeRequest(POST, routes.PersonUKPassportController.post(1).url)
              .withFormUrlEncodedBody(
                "ukPassport"       -> "true",
                "ukPassportNumber" -> ukPassportNumber
              )

            val responsiblePeople = ResponsiblePerson(
              ukPassport = Some(UKPassportNo),
              dateOfBirth = Some(DateOfBirth(LocalDate.of(2001, 12, 1)))
            )

            when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName = Some(personName))))))

            when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
              .thenReturn(Some(Seq(responsiblePeople)))

            when(controller.dataCacheConnector.fetchAll(any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(controller.dataCacheConnector.save(any(), any(), any())(any()))
              .thenReturn(Future.successful(mockCacheMap))

            val result = controller.post(1, true, Some(flowFromDeclaration))(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(
              Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1, Some(flowFromDeclaration)).url)
            )

          }

          "non uk passport has not been changed" in new Fixture {

            val newRequest = FakeRequest(POST, routes.PersonUKPassportController.post(1).url)
              .withFormUrlEncodedBody(
                "ukPassport" -> "false"
              )

            val responsiblePeople = ResponsiblePerson(
              ukPassport = Some(UKPassportNo)
            )

            when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName = Some(personName))))))

            when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
              .thenReturn(Some(Seq(responsiblePeople)))

            when(controller.dataCacheConnector.fetchAll(any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(controller.dataCacheConnector.save(any(), any(), any())(any()))
              .thenReturn(Future.successful(mockCacheMap))

            val result = controller.post(1, true, Some(flowFromDeclaration))(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(
              Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1, Some(flowFromDeclaration)).url)
            )

          }
        }
      }

      "given invalid data" must {
        "respond with BAD_REQUEST" in new Fixture {

          val newRequest = FakeRequest(POST, routes.PersonUKPassportController.post(1).url)
            .withFormUrlEncodedBody(
              "ukPassport"       -> "true",
              "ukPassportNumber" -> "abc"
            )

          val responsiblePeople = ResponsiblePerson()

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName = Some(personName))))))

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
            .thenReturn(Some(Seq(responsiblePeople)))

          when(controller.dataCacheConnector.fetchAll(any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.dataCacheConnector.save(any(), any(), any())(any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(1)(newRequest)
          status(result) must be(BAD_REQUEST)

        }
      }

      "Responsible Person cannot be found with given index" must {
        "respond with NOT_FOUND" in new Fixture {

          val newRequest = FakeRequest(POST, routes.PersonUKPassportController.post(1).url)
            .withFormUrlEncodedBody(
              "ukPassport" -> "false"
            )

          val responsiblePeople = ResponsiblePerson()

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePerson(personName = Some(personName))))))

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
            .thenReturn(Some(Seq(responsiblePeople)))

          when(controller.dataCacheConnector.fetchAll(any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(controller.dataCacheConnector.save(any(), any(), any())(any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(10)(newRequest)
          status(result) must be(NOT_FOUND)

        }
      }

    }

  }

  it must {
    "remove non uk passport data excluding date of birth" when {
      "data is changed to uk passport" in new Fixture {

        val dateOfBirth = DateOfBirth(LocalDate.of(2000, 1, 1))

        val newRequest = FakeRequest(POST, routes.PersonUKPassportController.post(1).url)
          .withFormUrlEncodedBody(
            "ukPassport"       -> "true",
            "ukPassportNumber" -> ukPassportNumber
          )

        val responsiblePeople = ResponsiblePerson(
          ukPassport = Some(UKPassportNo),
          nonUKPassport = Some(NoPassport),
          dateOfBirth = Some(dateOfBirth)
        )

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
          .thenReturn(
            Future.successful(
              Some(Seq(ResponsiblePerson(personName = Some(personName), dateOfBirth = Some(dateOfBirth))))
            )
          )

        when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
          .thenReturn(Some(Seq(responsiblePeople)))

        when(controller.dataCacheConnector.fetchAll(any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(controller.dataCacheConnector.save(any(), any(), any())(any()))
          .thenReturn(Future.successful(mockCacheMap))

        val result = controller.post(1, true)(newRequest)
        status(result) must be(SEE_OTHER)

        verify(controller.dataCacheConnector)
          .save[Seq[ResponsiblePerson]](
            any(),
            any(),
            meq(
              Seq(
                responsiblePeople.copy(
                  ukPassport = Some(UKPassportYes(ukPassportNumber)),
                  nonUKPassport = None,
                  dateOfBirth = Some(dateOfBirth),
                  hasChanged = true
                )
              )
            )
          )(any())

      }
    }
  }
}
