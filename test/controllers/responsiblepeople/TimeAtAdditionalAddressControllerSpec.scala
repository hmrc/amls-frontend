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
import utils.AmlsSpec
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class TimeAtAdditionalAddressControllerSpec extends AmlsSpec with MockitoSugar {

  val mockDataCacheConnector = mock[DataCacheConnector]
  val RecordId = 1

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val timeAtAdditionalAddressController = new TimeAtAdditionalAddressController (
      dataCacheConnector = mockDataCacheConnector,
      authAction = SuccessfulAuthAction, ds = commonDependencies, cc = mockMcc)
  }

  val emptyCache = CacheMap("", Map.empty)
  val outOfBounds = 99

  "TimeAtAdditionalAddressController" when {

    val personName = Some(PersonName("firstname", None, "lastname"))

    "get is called" must {

      "display the page" when {
        "with existing data" in new Fixture {

          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(personName = personName, addressHistory = Some(history))

          when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = timeAtAdditionalAddressController.get(RecordId)(request)
          status(result) must be(OK)

          val document: Document = Jsoup.parse(contentAsString(result))

          document.select("input[type=radio][name=timeAtAddress][value=01]").hasAttr("checked") must be(true)
          document.select("input[type=radio][name=timeAtAddress][value=02]").hasAttr("checked") must be(false)
          document.select("input[type=radio][name=timeAtAddress][value=03]").hasAttr("checked") must be(false)
          document.select("input[type=radio][name=timeAtAddress][value=04]").hasAttr("checked") must be(false)

        }
        "timeAtAddress has not been previously saved" in new Fixture {

          val responsiblePeople = ResponsiblePerson(personName)

          when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val result = timeAtAdditionalAddressController.get(RecordId)(request)
          status(result) must be(OK)

          val document: Document = Jsoup.parse(contentAsString(result))

          document.select("input[type=radio][name=timeAtAddress][value=01]").hasAttr("checked") must be(false)
          document.select("input[type=radio][name=timeAtAddress][value=02]").hasAttr("checked") must be(false)
          document.select("input[type=radio][name=timeAtAddress][value=03]").hasAttr("checked") must be(false)
          document.select("input[type=radio][name=timeAtAddress][value=04]").hasAttr("checked") must be(false)

        }

      }


      "respond with NOT_FOUND when called with an index that is out of bounds" in new Fixture {

        val responsiblePeople = ResponsiblePerson()

        when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = timeAtAdditionalAddressController.get(outOfBounds)(request)
        status(result) must be(NOT_FOUND)
      }

    }

    "post is called" must {
      "respond with SEE_OTHER" when {

        "a time at address has been selected" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "timeAtAddress" -> "04"
          )
          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

          when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = timeAtAdditionalAddressController.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
        }

      }

      "respond with BAD_REQUEST" when {
        "no time has been selected" in new Fixture {

          val requestWithMissingParams = request.withFormUrlEncodedBody(
            "timeAtAddress" -> ""
          )

          when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = timeAtAdditionalAddressController.post(RecordId)(requestWithMissingParams)
          status(result) must be(BAD_REQUEST)
        }
      }

      "respond with NOT_FOUND" when {
        "given an index out of bounds in edit mode" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "timeAtAddress" -> "01"
          )

          when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePerson()))))
          when(timeAtAdditionalAddressController.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())(any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = timeAtAdditionalAddressController.post(outOfBounds, true)(requestWithParams)

          status(result) must be(NOT_FOUND)
        }
      }

      "when edit mode is on" when {
        "time at address is less than 1 year" must {
          "redirect to the correct location" in new Fixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "timeAtAddress" -> "01"
            )
            val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
            val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
            val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
            val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

            when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
            when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = timeAtAdditionalAddressController.post(RecordId, true)(requestWithParams)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.AdditionalExtraAddressController.get(RecordId, true).url))
          }
        }

        "time at address is OneToThreeYears" must {
          "redirect to the correct location" in new Fixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "timeAtAddress" -> "03"
            )
            val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
            val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
            val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
            val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

            when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
            when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = timeAtAdditionalAddressController.post(RecordId, true, Some(flowFromDeclaration))(requestWithParams)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(RecordId, Some(flowFromDeclaration)).url))
          }
        }
        "time at address is ThreeYearsPlus" must {
          "redirect to the correct location" in new Fixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "timeAtAddress" -> "04"
            )
            val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
            val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
            val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
            val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

            when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
            when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = timeAtAdditionalAddressController.post(RecordId, true, Some(flowFromDeclaration))(requestWithParams)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(RecordId, Some(flowFromDeclaration)).url))
          }
        }
      }

      "when edit mode is off" when {
        "time at address is less than 1 year" must {
          "redirect to the correct location" in new Fixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "timeAtAddress" -> "01"
            )
            val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
            val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
            val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
            val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

            when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
            when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = timeAtAdditionalAddressController.post(RecordId)(requestWithParams)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.AdditionalExtraAddressController.get(RecordId).url))
          }
        }

        "time at address is OneToThreeYears" must {
          "redirect to the correct location" in new Fixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "timeAtAddress" -> "03"
            )
            val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
            val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
            val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
            val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))
            when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
            when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = timeAtAdditionalAddressController.post(RecordId)(requestWithParams)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.PositionWithinBusinessController.get(RecordId).url))
          }
        }

        "time at address is ThreeYearsPlus" must {
          "redirect to the correct location" in new Fixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "timeAtAddress" -> "04"
            )
            val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
            val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
            val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
            val responsiblePeople = ResponsiblePerson(addressHistory = Some(history))

            when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any(), any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
            when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any(), any())(any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = timeAtAdditionalAddressController.post(RecordId)(requestWithParams)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.PositionWithinBusinessController.get(RecordId).url))
          }
        }
      }
    }
  }
}
