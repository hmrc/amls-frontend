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

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.Country
import models.responsiblepeople.ResponsiblePeople._
import models.responsiblepeople.TimeAtAddress.{SixToElevenMonths, ZeroToFiveMonths}
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar

import scala.collection.JavaConversions._
import utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class TimeAtAdditionalExtraAddressControllerSpec extends GenericTestHelper with MockitoSugar {

  val mockDataCacheConnector = mock[DataCacheConnector]
  val RecordId = 1

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val timeAtAdditionalExtraAddressController = new TimeAtAdditionalExtraAddressController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
    }
  }

  val mockCacheMap = mock[CacheMap]
  val emptyCache = CacheMap("", Map.empty)
  val outOfBounds = 99

  "TimeAtAdditionalExtraAddressController" when {

    val personName = Some(PersonName("firstname", None, "lastname", None, None))

    "get is called" must {
      "display status 200" when {
        "without existing data" in new Fixture {

          val responsiblePeople = ResponsiblePeople(personName)

          when(timeAtAdditionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
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

          val personName = Some(PersonName("firstname", None, "lastname", None, None))

          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(additionalExtraAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(personName = personName, addressHistory = Some(history))

          when(timeAtAdditionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
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

          val responsiblePeople = ResponsiblePeople()

          when(timeAtAdditionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = timeAtAdditionalExtraAddressController.get(outOfBounds)(request)
          status(result) must be(NOT_FOUND)
        }
      }
    }

    "post is called" must {

      "go to DetailedAnswersController" when {
        "edit is true" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "timeAtAddress" -> "02"
          )

          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(additionalExtraAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

          when(timeAtAdditionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(timeAtAdditionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = timeAtAdditionalExtraAddressController.post(RecordId, true, Some(flowFromDeclaration))(requestWithParams)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(RecordId, true, Some(flowFromDeclaration)).url))
        }
      }

      "go to PositionWithinBusinessController" when {
        "edit is false" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "timeAtAddress" -> "02"
          )

          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(additionalExtraAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

          when(timeAtAdditionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(timeAtAdditionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = timeAtAdditionalExtraAddressController.post(RecordId)(requestWithParams)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.PositionWithinBusinessController.get(RecordId).url))
        }
      }

      "respond with BAD_REQUEST on submission" when {

        "given an invalid form" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "timeAtAddress" -> ""
          )

          val responsiblePeople = ResponsiblePeople()

          when(timeAtAdditionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(timeAtAdditionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = timeAtAdditionalExtraAddressController.post(RecordId, true)(requestWithParams)

          status(result) mustBe BAD_REQUEST

        }

      }

      "respond with NOT_FOUND" when {
        "an addressExtraAddress is not stored for that index" in new Fixture {
          val requestWithParams = request.withFormUrlEncodedBody(
            "timeAtAddress" -> "03"
          )

          when(timeAtAdditionalExtraAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))
          when(timeAtAdditionalExtraAddressController.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = timeAtAdditionalExtraAddressController.post(RecordId)(requestWithParams)
          status(result) must be(NOT_FOUND)
        }
      }
    }

  }

  it must {
    "use the correct services" in new Fixture {
      TimeAtAdditionalExtraAddressController.dataCacheConnector must be(DataCacheConnector)
      TimeAtAdditionalExtraAddressController.authConnector must be(AMLSAuthConnector)
    }
  }

}
