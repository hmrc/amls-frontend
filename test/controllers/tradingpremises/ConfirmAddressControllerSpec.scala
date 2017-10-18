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
import generators.businessmatching.BusinessMatchingGenerator
import generators.tradingpremises.TradingPremisesGenerator
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
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper, RepeatingSection}

import scala.concurrent.Future

class ConfirmAddressControllerSpec extends GenericTestHelper with MockitoSugar with TradingPremisesGenerator with BusinessMatchingGenerator {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>
    val request = addToken(authRequest)
    val dataCache: DataCacheConnector = mockCacheConnector
    val controller = new ConfirmAddressController(messagesApi, self.dataCache, self.authConnector)

    mockCacheFetchAll
  }

  "ConfirmTradingPremisesAddress" must {

    val bm = businessMatchingGen.sample.get

    "Get Option:" must {

      "Load Confirm trading premises address page successfully" when {
        "YourTradingPremises is not set" in new Fixture {
          val tp = tradingPremisesGen.sample.get.copy(yourTradingPremises = None)

          mockCacheGetEntry(Some(bm), BusinessMatching.key)
          mockCacheGetEntry(Some(Seq(tp)), TradingPremises.key)

          val result = controller.get(1)(request)

          status(result) must be(OK)
          contentAsString(result) must include(Messages(bm.reviewDetails.get.businessAddress.line_1))
        }
      }

      "redirect to where is your trading premises page" when {
        "business matching model does not exist" in new Fixture {
          mockCacheGetEntry(None, BusinessMatching.key)

          val result = controller.get(1)(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1).url))
        }

        "business matching -> review details is empty" in new Fixture {
          mockCacheGetEntry(Some(bm.copy(reviewDetails = None)), BusinessMatching.key)

          val result = controller.get(1)(request)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1).url))
        }

        "the trading premises already has a 'your trading premises' model set" in new Fixture {
          mockCacheGetEntry(Some(bm), BusinessMatching.key)
          mockCacheGetEntry(Some(Seq(tradingPremisesGen.sample.get)), TradingPremises.key)

          val result = controller.get(1)(request)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1).url))
        }
      }
    }

    "Post" must {

      val ytp = (bm.reviewDetails map { rd =>
        YourTradingPremises(
          rd.businessName,
          Address(
            rd.businessAddress.line_1,
            rd.businessAddress.line_2,
            rd.businessAddress.line_3,
            rd.businessAddress.line_4,
            rd.businessAddress.postcode.get
          )
        )
      }).get

      "successfully redirect to next page" when {

        "option is 'Yes' is selected confirming the mentioned address is the trading premises address" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "confirmAddress" -> "true"
          )

          mockCacheGetEntry(Some(Seq(TradingPremises())), TradingPremises.key)
          mockCacheGetEntry(Some(bm), BusinessMatching.key)

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

          mockCacheGetEntry(Some(Seq(TradingPremises(yourTradingPremises = Some(mock[YourTradingPremises])))), TradingPremises.key)
          mockCacheGetEntry(Some(bm), BusinessMatching.key)

          val result = controller.post(1)(newRequest)
          status(result) must be (SEE_OTHER)
          redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1).url))
        }

      }

      "throw error message on not selecting the option" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody()

        mockCacheFetch[BusinessMatching](Some(bm))

        val result = controller.post(1)(newRequest)
        status(result) must be(BAD_REQUEST)
      }

    }
  }
}
