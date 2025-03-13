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

package controllers.responsiblepeople.address

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.responsiblepeople.address.TimeAtAddressFormProvider
import models.responsiblepeople.ResponsiblePerson._
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, SixToElevenMonths, ThreeYearsPlus, ZeroToFiveMonths}
import models.responsiblepeople._
import models.status.SubmissionReadyForReview
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.StatusService
import services.cache.Cache
import utils.{AmlsSpec, AuthorisedFixture}
import views.html.responsiblepeople.address.TimeAtAddressView

import scala.concurrent.Future

class TimeAtCurrentAddressControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  val mockDataCacheConnector = mock[DataCacheConnector]
  val mockStatusService      = mock[StatusService]
  val recordId               = 1

  trait Fixture extends AuthorisedFixture {
    self =>
    val request                        = addToken(authRequest)
    lazy val view                      = inject[TimeAtAddressView]
    val timeAtCurrentAddressController = new TimeAtCurrentAddressController(
      dataCacheConnector = mockDataCacheConnector,
      authAction = SuccessfulAuthAction,
      statusService = mockStatusService,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[TimeAtAddressFormProvider],
      view = view,
      error = errorView
    )

    when(timeAtCurrentAddressController.statusService.getStatus(Some(any()), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(SubmissionReadyForReview))
  }

  val mockCacheMap = mock[Cache]
  val emptyCache   = Cache.empty
  val outOfBounds  = 99

  "TimeAtCurrentAddressController" when {

    val personName = Some(PersonName("firstname", None, "lastname"))

    "get is called" must {
      "display status 200" when {
        "without existing data" in new Fixture {

          val responsiblePeople = ResponsiblePerson(personName)

          when(timeAtCurrentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = timeAtCurrentAddressController.get(recordId)(request)
          status(result) must be(OK)

          val document: Document = Jsoup.parse(contentAsString(result))

          TimeAtAddress.all.foreach { item =>
            document.getElementById(item.toString).hasAttr("checked") must be(false)
          }
        }

        "with existing data" in new Fixture {

          val personName = Some(PersonName("firstname", None, "lastname"))

          val UKAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA1 1AA")
          val currentAddress    = ResponsiblePersonCurrentAddress(UKAddress, Some(ZeroToFiveMonths))
          val history           = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress))
          val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

          when(timeAtCurrentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = timeAtCurrentAddressController.get(recordId)(request)
          status(result) must be(OK)

          val document: Document = Jsoup.parse(contentAsString(result))

          TimeAtAddress.all.foreach { item =>
            if (item.toString == ZeroToFiveMonths.toString) {
              document.getElementById(item.toString).hasAttr("checked") must be(true)
            } else {
              document.getElementById(item.toString).hasAttr("checked") must be(false)
            }
          }
        }
      }

      "respond with NOT_FOUND" when {
        "called with an index that is out of bounds" in new Fixture {

          val responsiblePeople = ResponsiblePerson()

          when(timeAtCurrentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = timeAtCurrentAddressController.get(outOfBounds)(request)
          status(result) must be(NOT_FOUND)
        }
      }
    }

    "post is called" must {

      "go to DetailedAnswersController" when {
        "edit is true and answer is has lived at address over a year" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.TimeAtCurrentAddressController.post(1, true).url)
            .withFormUrlEncodedBody(
              "timeAtAddress" -> OneToThreeYears.toString
            )

          val UKAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA1 1AA")
          val currentAddress    = ResponsiblePersonCurrentAddress(UKAddress, Some(SixToElevenMonths))
          val history           = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress))
          val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

          when(timeAtCurrentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(
            timeAtCurrentAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any())
          )
            .thenReturn(Future.successful(mockCacheMap))

          val result = timeAtCurrentAddressController.post(recordId, true, Some(flowFromDeclaration))(requestWithParams)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(
              controllers.responsiblepeople.routes.DetailedAnswersController
                .get(recordId, Some(flowFromDeclaration))
                .url
            )
          )
        }
      }

      "go to PositionWithinBusinessController" when {
        "edit is false and more than 3 years" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.TimeAtCurrentAddressController.post(1).url)
            .withFormUrlEncodedBody(
              "timeAtAddress" -> ThreeYearsPlus.toString
            )

          val UKAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA1 1AA")
          val currentAddress    = ResponsiblePersonCurrentAddress(UKAddress, Some(ThreeYearsPlus))
          val history           = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress))
          val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

          when(timeAtCurrentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(
            timeAtCurrentAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any())
          )
            .thenReturn(Future.successful(mockCacheMap))

          val result = timeAtCurrentAddressController.post(recordId)(requestWithParams)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.responsiblepeople.routes.PositionWithinBusinessController.get(recordId).url)
          )
        }
      }

      "go to AdditionalAddressController" when {
        "edit is false and less than 1 year" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.TimeAtCurrentAddressController.post(1).url)
            .withFormUrlEncodedBody(
              "timeAtAddress" -> SixToElevenMonths.toString
            )

          val UKAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA1 1AA")
          val currentAddress    = ResponsiblePersonCurrentAddress(UKAddress, Some(SixToElevenMonths))
          val history           = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress))
          val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

          when(timeAtCurrentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(
            timeAtCurrentAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any())
          )
            .thenReturn(Future.successful(mockCacheMap))

          val result = timeAtCurrentAddressController.post(recordId)(requestWithParams)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.AdditionalAddressController.get(recordId).url))
        }

        "edit is true and less than 1 year" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.TimeAtCurrentAddressController.post(1, true).url)
            .withFormUrlEncodedBody(
              "timeAtAddress" -> SixToElevenMonths.toString
            )

          val UKAddress         = PersonAddressUK("Line 1", Some("Line 2"), Some("Line 3"), None, "AA1 1AA")
          val currentAddress    = ResponsiblePersonCurrentAddress(UKAddress, Some(SixToElevenMonths))
          val history           = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress))
          val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

          when(timeAtCurrentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(
            timeAtCurrentAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any())
          )
            .thenReturn(Future.successful(mockCacheMap))

          val result = timeAtCurrentAddressController.post(recordId, true)(requestWithParams)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.AdditionalAddressController.get(recordId, true).url))
        }
      }

      "respond with BAD_REQUEST on submission" when {

        "given an invalid form" in new Fixture {

          val requestWithParams = FakeRequest(POST, routes.TimeAtCurrentAddressController.post(1).url)
            .withFormUrlEncodedBody(
              "timeAtAddress" -> ""
            )

          val responsiblePeople = ResponsiblePerson()

          when(timeAtCurrentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(
            timeAtCurrentAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any())
          )
            .thenReturn(Future.successful(mockCacheMap))

          val result = timeAtCurrentAddressController.post(recordId, true)(requestWithParams)

          status(result) mustBe BAD_REQUEST

        }

      }

      "respond with NOT_FOUND" when {
        "a current address is not stored for that index" in new Fixture {
          val requestWithParams = FakeRequest(POST, routes.TimeAtCurrentAddressController.post(1).url)
            .withFormUrlEncodedBody(
              "timeAtAddress" -> OneToThreeYears.toString
            )

          when(timeAtCurrentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))
          when(
            timeAtCurrentAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any())
          )
            .thenReturn(Future.successful(mockCacheMap))

          val result = timeAtCurrentAddressController.post(recordId)(requestWithParams)
          status(result) must be(NOT_FOUND)
        }
      }
    }
  }
}
