/*
 * Copyright 2021 HM Revenue & Customs
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
import models.responsiblepeople.ResponsiblePerson._
import models.responsiblepeople.TimeAtAddress.{SixToElevenMonths, ThreeYearsPlus, ZeroToFiveMonths}
import models.responsiblepeople._
import models.status.SubmissionReadyForReview
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture}
import views.html.responsiblepeople.address.time_at_address

import scala.concurrent.Future

class TimeAtCurrentAddressControllerSpec extends AmlsSpec with MockitoSugar {

  val mockDataCacheConnector = mock[DataCacheConnector]
  val mockStatusService = mock[StatusService]
  val RecordId = 1

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)
    lazy val view = app.injector.instanceOf[time_at_address]
    val timeAtCurrentAddressController = new TimeAtCurrentAddressController (
      dataCacheConnector = mockDataCacheConnector,
      authAction = SuccessfulAuthAction,
      statusService = mockStatusService,
      ds = commonDependencies,
      cc = mockMcc,
      time_at_address = view,
      error = errorView
    )

    when(timeAtCurrentAddressController.statusService.getStatus(Some(any()), any(), any())(any(), any()))
      .thenReturn(Future.successful(SubmissionReadyForReview))
  }

  val mockCacheMap = mock[CacheMap]
  val emptyCache = CacheMap("", Map.empty)
  val outOfBounds = 99

  "TimeAtCurrentAddressController" when {

    val personName = Some(PersonName("firstname", None, "lastname"))

    "get is called" must {
      "display status 200" when {
        "without existing data" in new Fixture {

          val responsiblePeople = ResponsiblePerson(personName)

          when(timeAtCurrentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = timeAtCurrentAddressController.get(RecordId)(request)
          status(result) must be(OK)

          val document: Document = Jsoup.parse(contentAsString(result))

          document.select("input[type=radio][name=timeAtAddress][value=01]").hasAttr("checked") must be(false)
          document.select("input[type=radio][name=timeAtAddress][value=02]").hasAttr("checked") must be(false)
          document.select("input[type=radio][name=timeAtAddress][value=03]").hasAttr("checked") must be(false)
          document.select("input[type=radio][name=timeAtAddress][value=04]").hasAttr("checked") must be(false)
        }

        "with existing data" in new Fixture {

          val personName = Some(PersonName("firstname", None, "lastname"))

          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val currentAddress = ResponsiblePersonCurrentAddress(UKAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress))
          val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

          when(timeAtCurrentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = timeAtCurrentAddressController.get(RecordId)(request)
          status(result) must be(OK)

          val document: Document = Jsoup.parse(contentAsString(result))

          document.select("input[type=radio][name=timeAtAddress][value=01]").hasAttr("checked") must be(true)
          document.select("input[type=radio][name=timeAtAddress][value=02]").hasAttr("checked") must be(false)
          document.select("input[type=radio][name=timeAtAddress][value=03]").hasAttr("checked") must be(false)
          document.select("input[type=radio][name=timeAtAddress][value=04]").hasAttr("checked") must be(false)
        }
      }

      "respond with NOT_FOUND" when {
        "called with an index that is out of bounds" in new Fixture {

          val responsiblePeople = ResponsiblePerson()

          when(timeAtCurrentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = timeAtCurrentAddressController.get(outOfBounds)(request)
          status(result) must be(NOT_FOUND)
        }
      }
    }

    "post is called" must {

      "go to DetailedAnswersController" when {
        "edit is true" in new Fixture {

          val requestWithParams = requestWithUrlEncodedBody(
            "timeAtAddress" -> "03"
          )

          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val currentAddress = ResponsiblePersonCurrentAddress(UKAddress, Some(SixToElevenMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress))
          val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

          when(timeAtCurrentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(timeAtCurrentAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = timeAtCurrentAddressController.post(RecordId, true, Some(flowFromDeclaration))(requestWithParams)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(RecordId, Some(flowFromDeclaration)).url))
        }
      }

      "go to PositionWithinBusinessController" when {
        "edit is false and more than 3 years" in new Fixture {

          val requestWithParams = requestWithUrlEncodedBody(
            "timeAtAddress" -> "04"
          )

          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val currentAddress = ResponsiblePersonCurrentAddress(UKAddress, Some(ThreeYearsPlus))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress))
          val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

          when(timeAtCurrentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(timeAtCurrentAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = timeAtCurrentAddressController.post(RecordId)(requestWithParams)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.PositionWithinBusinessController.get(RecordId).url))
        }
      }

      "go to AdditionalAddressController" when {
        "edit is false and less than 3 years" in new Fixture {

          val requestWithParams = requestWithUrlEncodedBody(
            "timeAtAddress" -> "03"
          )

          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val currentAddress = ResponsiblePersonCurrentAddress(UKAddress, Some(SixToElevenMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(currentAddress))
          val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

          when(timeAtCurrentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(timeAtCurrentAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = timeAtCurrentAddressController.post(RecordId)(requestWithParams)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.PositionWithinBusinessController.get(RecordId).url))
        }
      }

      "respond with BAD_REQUEST on submission" when {

        "given an invalid form" in new Fixture {

          val requestWithParams = requestWithUrlEncodedBody(
            "timeAtAddress" -> ""
          )

          val responsiblePeople = ResponsiblePerson()

          when(timeAtCurrentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(timeAtCurrentAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = timeAtCurrentAddressController.post(RecordId, true)(requestWithParams)

          status(result) mustBe BAD_REQUEST

        }

      }

      "respond with NOT_FOUND" when {
        "a current address is not stored for that index" in new Fixture {
          val requestWithParams = requestWithUrlEncodedBody(
            "timeAtAddress" -> "03"
          )

          when(timeAtCurrentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))
          when(timeAtCurrentAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = timeAtCurrentAddressController.post(RecordId)(requestWithParams)
          status(result) must be(NOT_FOUND)
        }
      }
    }
  }
}
