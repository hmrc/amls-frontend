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

package controllers.businessmatching.updateservice.add

import controllers.businessmatching.updateservice.UpdateServiceHelper
import generators.tradingpremises.TradingPremisesGenerator
import models.businessmatching._
import models.businessmatching.updateservice.{TradingPremisesActivities, UpdateService}
import models.flowmanagement.{AddServiceFlowModel, WhichTradingPremisesPageId}
import models.tradingpremises.TradingPremises
import org.scalacheck.Gen
import org.scalatest.PrivateMethodTester
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

class WhichTradingPremisesControllerSpec extends GenericTestHelper
  with PrivateMethodTester
  with TradingPremisesGenerator {

  sealed trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val request = addToken(authRequest)

    // scalastyle:off magic.number
    val tradingPremises = Gen.listOfN(5, tradingPremisesGen).sample.get

    mockCacheSave[Seq[TradingPremises]]
    mockCacheSave[UpdateService]
    mockCacheFetch[AddServiceFlowModel](Some(AddServiceFlowModel(Some(HighValueDealing), Some(true))), Some(AddServiceFlowModel.key))
    val mockUpdateServiceHelper = mock[UpdateServiceHelper]
    val mockBusinessMatchingService = mock[BusinessMatchingService]

    val controller = new WhichTradingPremisesController(
      authConnector = self.authConnector,
      dataCacheConnector = mockCacheConnector,
      statusService = mockStatusService,
      businessMatchingService = mockBusinessMatchingService,
      helper = mockUpdateServiceHelper,
      router = createRouter[AddServiceFlowModel]
    )
  }

  "WhichTradingPremisesController" when {

    "get is called" must {
      "return OK with which_trading_premises view" in new Fixture {
        mockCacheFetch[Seq[TradingPremises]](Some(tradingPremises), Some(TradingPremises.key))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(
          Messages(
            "businessmatching.updateservice.whichtradingpremises.heading",
            Messages(s"businessmatching.registerservices.servicename.lbl.${BusinessActivities.getValue(HighValueDealing)}.phrased")
          ))
      }

      "return INTERNAL_SERVER_ERROR" when {
        "activities cannot be retrieved" in new Fixture {
          mockCacheFetch[AddServiceFlowModel](Some(AddServiceFlowModel()), Some(AddServiceFlowModel.key))
          mockCacheFetch[Seq[TradingPremises]](Some(tradingPremises), Some(TradingPremises.key))

          val result = controller.get()(request)
          status(result) must be(INTERNAL_SERVER_ERROR)
        }
      }
    }

    "post" must {

      "on valid request" must {
        "redirect away" when {
          "trading premises are selected" in new Fixture {
            mockCacheFetch[Seq[TradingPremises]](Some(tradingPremises), Some(TradingPremises.key))
            mockCacheUpdate(Some(AddServiceFlowModel.key), AddServiceFlowModel())

            val result = controller.post()(request.withFormUrlEncodedBody(
              "tradingPremises[]" -> "1"
            ))

            status(result) must be(SEE_OTHER)

            controller.router.verify(WhichTradingPremisesPageId,
              AddServiceFlowModel(tradingPremisesActivities = Some(TradingPremisesActivities(Set(1))), hasChanged = true))
          }
        }
      }

      "on invalid request" must {
        "return BAD_REQUEST" in new Fixture {
          mockCacheFetch[Seq[TradingPremises]](Some(tradingPremises), Some(TradingPremises.key))

          val result = controller.post()(request)

          status(result) must be(BAD_REQUEST)
        }
      }

      "return INTERNAL_SERVER_ERROR" when {
        "activities cannot be retrieved" in new Fixture {
          mockCacheFetch[AddServiceFlowModel](Some(AddServiceFlowModel()), Some(AddServiceFlowModel.key))

          val result = controller.post()(request)

          status(result) must be(INTERNAL_SERVER_ERROR)
        }
      }
    }
  }
}