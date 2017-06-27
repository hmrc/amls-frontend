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

package controllers.tradingpremises

import connectors.DataCacheConnector
import models.Country
import models.businesscustomer.{ReviewDetails, Address => BCAddress}
import models.businessmatching.{BusinessMatching, BusinessType}
import models.tradingpremises.{Address, AgentCompanyDetails, TradingPremises, YourTradingPremises}
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import play.api.test.Helpers._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{AuthorisedFixture, GenericTestHelper, RepeatingSection}

import scala.concurrent.Future


class ConfirmAddressControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)
    val dataCache: DataCacheConnector = mock[DataCacheConnector]
    val controller = new ConfirmAddressController(messagesApi, self.dataCache, self.authConnector)

  }

  "ConfirmTradingPremisesAddress" must {

    val reviewDtls = ReviewDetails("BusinessName", Some(BusinessType.LimitedCompany),
      BCAddress("line1", "line2", Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB")), "ghghg")
    val bm = BusinessMatching(Some(reviewDtls))


    "Get Option:" must {

      "Load Confirm trading premises address page successfully" in new Fixture {

        when(controller.dataCacheConnector.fetch[BusinessMatching](any())(any(), any(), any())).thenReturn(Future.successful(Some(bm)))
        val result = controller.get(1)(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("line1"))
      }

      "redirect to where is your trading premises page" when {
        "business matching model does not exist" in new Fixture {

          when(controller.dataCacheConnector.fetch[BusinessMatching](any())(any(), any(), any())).thenReturn(Future.successful(None))
          val result = controller.get(1)(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1).url))
        }

        "business matching ->review details is empty" in new Fixture {
          when(controller.dataCacheConnector.fetch[BusinessMatching](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(bm.copy(reviewDetails = None))))
          val result = controller.get(1)(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1).url))
        }
      }
    }

    "Post" must {

      val ytp = YourTradingPremises(
        "BusinessName",
        Address(
          "line1",
          "line2",
          Some("line3"),
          Some("line4"),
          "AA1 1AA"
        )
      )

      "successfully redirect to next page" when {

        "option is 'Yes' is selected confirming the mentioned address is the trading premises address" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "confirmAddress" -> "true"
          )

          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(TradingPremises())))

          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(bm))

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post(1)(newRequest)
          status(result) must be (SEE_OTHER)
          redirectLocation(result) must be(Some(routes.ActivityStartDateController.get(1).url))

          verify(controller.dataCacheConnector).save[Seq[TradingPremises]](
            any(),
            meq(Seq(TradingPremises(yourTradingPremises = Some(ytp)))))(any(), any(), any())

        }

        "option is 'No' is selected confirming the mentioned address is the trading premises address" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "confirmAddress" -> "false"
          )

          val mockCacheMap = mock[CacheMap]

          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(TradingPremises(yourTradingPremises = Some(mock[YourTradingPremises])))))

          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(bm))

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post(1)(newRequest)
          status(result) must be (SEE_OTHER)
          redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1).url))

          verify(controller.dataCacheConnector).save[Seq[TradingPremises]](
            any(),
            meq(Seq(TradingPremises(yourTradingPremises = None))))(any(), any(), any())

        }

      }

      "throw error message on not selecting the option" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody(
        )
        when(controller.dataCacheConnector.fetch[BusinessMatching](any())(any(),any(),any()))
          .thenReturn(Future.successful(Some(bm)))

        val result = controller.post(1)(newRequest)
        status(result) must be(BAD_REQUEST)
      }

    }
  }
}
