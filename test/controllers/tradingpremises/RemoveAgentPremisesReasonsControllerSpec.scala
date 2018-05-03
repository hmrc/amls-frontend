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

package controllers.tradingpremises

import connectors.DataCacheConnector
import models.tradingpremises.TradingPremises
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.JsValue
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, AmlsSpec}

import scala.concurrent.Future

class RemoveAgentPremisesReasonsControllerSpec extends AmlsSpec with MockitoSugar {

  import models.tradingpremises.RemovalReasonConstants._

  trait Fixture extends AuthorisedFixture {
    self =>

    implicit val request = addToken(authRequest)

    val controller = new RemoveAgentPremisesReasonsController {
      override val dataCacheConnector: DataCacheConnector = mock[DataCacheConnector]

      override protected def authConnector: AuthConnector = self.authConnector

      override val statusService: StatusService = mock[StatusService]
    }

    val tradingPremises = TradingPremises()
    val cache = CacheMap("", Map.empty[String, JsValue])

    def mockFetch(model: Option[Seq[TradingPremises]]) =
      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(model))

    when(controller.dataCacheConnector.save(eqTo(TradingPremises.key), any())(any(), any(), any())).
      thenReturn(Future.successful(cache))

    mockFetch(Some(Seq(tradingPremises)))
  }

  "Remove agent premises reasons controller" when {

    "invoking the GET action" must {

      "load the 'Why are you removing this trading premises?' page" in new Fixture {

        val result = controller.get(1)(request)
        status(result) must be(OK)

      }

      "return NOT FOUND when there is no trading premises found" in new Fixture {

        mockFetch(None)

        val result = controller.get(1)(request)
        status(result) must be(NOT_FOUND)

      }
    }

    "invoking the POST action" must {

      "return a bad request if there is a validation problem" in new Fixture {

        val formRequest = request.withFormUrlEncodedBody(
          "removalReason" -> Form.OTHER
        )

        val result = controller.post(1)(formRequest)

        status(result) must be(BAD_REQUEST)

      }

      "save the reason data to Save4Later" in new Fixture {

        val formRequest = request.withFormUrlEncodedBody(
          "removalReason" -> Form.OTHER,
          "removalReasonOther" -> "Some reason"
        )

        val result = await(controller.post(1)(formRequest))

        val captor = ArgumentCaptor.forClass(classOf[Seq[TradingPremises]])
        verify(controller.dataCacheConnector).save(eqTo(TradingPremises.key), captor.capture())(any(), any(), any())

        captor.getValue match {
          case tp :: tail =>
            tp.removalReason must be(Some(Schema.OTHER))
            tp.removalReasonOther must be(Some("Some reason"))
        }

      }

      "redirect to the 'Remove trading premises' page" in new Fixture {

        val formRequest = request.withFormUrlEncodedBody(
          "removalReason" -> Form.OTHER,
          "removalReasonOther" -> "Some reason"
        )

        val result = controller.post(1)(formRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.tradingpremises.routes.RemoveTradingPremisesController.get(1).url))

      }

    }
  }

}
