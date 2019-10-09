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
import models.responsiblepeople.ResponsiblePerson._
import models.responsiblepeople.TimeAtAddress.ZeroToFiveMonths
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture}

import scala.concurrent.Future

class TimeAtAdditionalExtraAddressControllerSpec extends AmlsSpec with MockitoSugar {

  val mockDataCacheConnector = mock[DataCacheConnector]
  val RecordId = 1

  trait Fixture {
    self =>
    val request = addToken(authRequest)

    val timeAtAdditionalExtraAddressController = new TimeAtAdditionalExtraAddressController (
      dataCacheConnector = mockDataCacheConnector,
      authAction = SuccessfulAuthAction, ds = commonDependencies, cc = mockMcc)
  }

  val mockCacheMap = mock[CacheMap]
  val emptyCache = CacheMap("", Map.empty)
  val outOfBounds = 99

  "TimeAtAdditionalExtraAddressController" when {

    val personName = Some(PersonName("firstname", None, "lastname"))

    "get is called" must {
      "display status 200" when {
        "without existing data" in new Fixture {

          val responsiblePeople = ResponsiblePerson(personName)

          when(timeAtAdditionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = timeAtAdditionalExtraAddressController.get(RecordId)(request)
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
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(additionalExtraAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

          when(timeAtAdditionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = timeAtAdditionalExtraAddressController.get(RecordId)(request)
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

          when(timeAtAdditionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = timeAtAdditionalExtraAddressController.get(outOfBounds)(request)
          status(result) must be(NOT_FOUND)
        }
      }
    }

    "post is called" must {

      "go to DetailedAnswersController" when {
        "edit is true" in new Fixture {

          val requestWithParams = requestWithUrlEncodedBody(
            "timeAtAddress" -> "02"
          )

          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(additionalExtraAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

          when(timeAtAdditionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(timeAtAdditionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = timeAtAdditionalExtraAddressController.post(RecordId, true, Some(flowFromDeclaration))(requestWithParams)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(RecordId, Some(flowFromDeclaration)).url))
        }
      }

      "go to PositionWithinBusinessController" when {
        "edit is false" in new Fixture {

          val requestWithParams = requestWithUrlEncodedBody(
            "timeAtAddress" -> "02"
          )

          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(additionalExtraAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

          when(timeAtAdditionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(timeAtAdditionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = timeAtAdditionalExtraAddressController.post(RecordId)(requestWithParams)
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

          when(timeAtAdditionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(timeAtAdditionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = timeAtAdditionalExtraAddressController.post(RecordId, true)(requestWithParams)

          status(result) mustBe BAD_REQUEST

        }

      }

      "respond with NOT_FOUND" when {
        "an addressExtraAddress is not stored for that index" in new Fixture {
          val requestWithParams = requestWithUrlEncodedBody(
            "timeAtAddress" -> "03"
          )

          when(timeAtAdditionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))
          when(timeAtAdditionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = timeAtAdditionalExtraAddressController.post(RecordId)(requestWithParams)
          status(result) must be(NOT_FOUND)
        }
      }
    }
  }
}
