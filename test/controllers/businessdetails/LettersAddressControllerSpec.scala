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

package controllers.businessdetails

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import models.businessdetails._
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito.{reset, _}
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture}

import scala.concurrent.Future

class LettersAddressControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with BeforeAndAfter {

  val dataCacheConnector = mock[DataCacheConnector]

  before {
    reset(dataCacheConnector)
  }

  trait Fixture {
    self => val request = addToken(authRequest)

    val controller = new LettersAddressController (
      dataCache = dataCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc)

    val emptyCache = CacheMap("", Map.empty)

    val mockCacheMap = mock[CacheMap]
  }

  private val ukAddress = RegisteredOfficeUK("line_1", "line_2", Some(""), Some(""), "AA1 1AA")
  private val businessDetails = BusinessDetails(None, None, None, None, None, None, Some(ukAddress), None,
    correspondenceAddressIsUk = Some(CorrespondenceAddressIsUk(true)),
    correspondenceAddress = Some(CorrespondenceAddress(Some(CorrespondenceAddressUk("", "", "", "", Some(""), Some(""), "")), None)))

  private val completeBusinessDetails = BusinessDetails(
    registeredOfficeIsUK = Some(RegisteredOfficeIsUK(true)),
    registeredOffice = Some(ukAddress),
    altCorrespondenceAddress = Some(true),
    correspondenceAddressIsUk = Some(CorrespondenceAddressIsUk(true)),
    correspondenceAddress = Some(CorrespondenceAddress(Some(CorrespondenceAddressUk("", "", "", "", Some(""), Some(""), "")), None)))

  "ConfirmRegisteredOfficeController" must {
    "Get" must {
      "load register Office" in new Fixture {
        when(controller.dataCache.fetch[BusinessDetails](any(), any())(any(),any()))
          .thenReturn(Future.successful(Some(businessDetails)))
        val result = controller.get()(request)
        status(result) must be(OK)
      }

      "load Registered office or main place of business when Business Address from mongoCache returns None" in new Fixture {
        when(controller.dataCache.fetch[BusinessDetails](any(), any())(any(),any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.businessdetails.routes.CorrespondenceAddressIsUkController.get().url))
      }
    }

    "Post" must {
      "remove the data for following questions and successfully redirect to the page on selection of 'Yes' [this is letters address]" in new Fixture {
        val newRequest = requestWithUrlEncodedBody(
          "lettersAddress" -> "true"
        )

        val expectedBusinessDetails = completeBusinessDetails.copy(altCorrespondenceAddress = Some(false),
          correspondenceAddressIsUk = None,
          correspondenceAddress = None,
          hasChanged = true,
          hasAccepted = false
        )

        when(controller.dataCache.fetchAll(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(mockCacheMap.getEntry[BusinessDetails](BusinessDetails.key))
          .thenReturn(Some(completeBusinessDetails))

        when (controller.dataCache.save(any(), any(), any())(any(), any())).thenReturn(Future.successful(emptyCache))


        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.businessdetails.routes.SummaryController.get().url))

        val captor = ArgumentCaptor.forClass(classOf[BusinessDetails])
        verify(controller.dataCache).save[BusinessDetails](any(), meq(BusinessDetails.key), captor.capture())(any(), any())

        captor.getValue match {
          case bd: BusinessDetails => bd must be(expectedBusinessDetails)
        }
      }

      "keep the data and successfully redirect to the page on selection of Option 'No' [this is not letters address]" in new Fixture {
        val newRequest = requestWithUrlEncodedBody(
          "lettersAddress" -> "false"
        )

        when(controller.dataCache.fetchAll(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(mockCacheMap.getEntry[BusinessDetails](BusinessDetails.key))
          .thenReturn(Some(completeBusinessDetails))

        when (controller.dataCache.save(any(), any(), any())(any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.businessdetails.routes.CorrespondenceAddressIsUkController.get().url))

        val captor = ArgumentCaptor.forClass(classOf[BusinessDetails])
        verify(controller.dataCache).save[BusinessDetails](any(), meq(BusinessDetails.key), captor.capture())(any(), any())

        captor.getValue.correspondenceAddressIsUk match {
          case Some(isUk) => isUk mustBe CorrespondenceAddressIsUk(true)
        }

        captor.getValue.correspondenceAddress match {
          case Some(correspondenceAddress) => correspondenceAddress.ukAddress mustBe defined
        }
      }

      "on post invalid data" in new Fixture {
        val newRequest = requestWithUrlEncodedBody(
        )

        when(controller.dataCache.fetch[BusinessDetails](any(), any())(any(),any()))
          .thenReturn(Future.successful(Some(businessDetails)))

        when (controller.dataCache.save(any(), any(), any())(any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
      }

      "on post with invalid data show error" in new Fixture {
        val newRequest = requestWithUrlEncodedBody(
          "lettersAddress" -> ""
        )
        when(controller.dataCache.fetch[BusinessDetails](any(), any())(any(),any()))
          .thenReturn(Future.successful(Some(businessDetails)))

        when (controller.dataCache.save(any(), any(), any())(any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
      }

      "on post with no data" in new Fixture {
        val newRequest = requestWithUrlEncodedBody(
        )

        when(controller.dataCache.fetch[BusinessDetails](any(), any())(any(),any()))
          .thenReturn(Future.successful(None))

        when (controller.dataCache.save(any(), any(), any())(any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.businessdetails.routes.CorrespondenceAddressIsUkController.get().url))
      }
    }
  }
}
