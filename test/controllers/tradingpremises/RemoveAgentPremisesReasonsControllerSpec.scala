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

package controllers.tradingpremises

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.tradingpremises.RemoveAgentPremisesReasonsFormProvider
import models.tradingpremises.AgentRemovalReason.Other
import models.tradingpremises.TradingPremises
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.AmlsSpec
import views.html.tradingpremises.RemoveAgentPremisesReasonsView

import scala.concurrent.Future

class RemoveAgentPremisesReasonsControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  import models.tradingpremises.RemovalReasonConstants._

  trait Fixture {
    self =>
    implicit val request: Request[AnyContentAsEmpty.type] = addToken(authRequest)
    lazy val view                                         = inject[RemoveAgentPremisesReasonsView]
    val controller                                        = new RemoveAgentPremisesReasonsController(
      dataCacheConnector = mock[DataCacheConnector],
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[RemoveAgentPremisesReasonsFormProvider],
      view = view,
      error = errorView
    )

    val tradingPremises = TradingPremises()
    val cache           = Cache.empty

    def mockFetch(model: Option[Seq[TradingPremises]]) =
      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
        .thenReturn(Future.successful(model))

    when(controller.dataCacheConnector.save(any(), eqTo(TradingPremises.key), any())(any()))
      .thenReturn(Future.successful(cache))

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

        val formRequest = FakeRequest(POST, routes.RemoveAgentPremisesReasonsController.post(1).url)
          .withFormUrlEncodedBody(
            "removalReason" -> Other.toString
          )

        val result = controller.post(1)(formRequest)

        status(result) must be(BAD_REQUEST)

      }

      "save the reason data to mongoCache" in new Fixture {

        val formRequest = FakeRequest(POST, routes.RemoveAgentPremisesReasonsController.post(1).url)
          .withFormUrlEncodedBody(
            "removalReason"      -> Other.toString,
            "removalReasonOther" -> "Some reason"
          )

        await(controller.post(1)(formRequest))

        val captor = ArgumentCaptor.forClass(classOf[Seq[TradingPremises]])
        verify(controller.dataCacheConnector).save(any(), eqTo(TradingPremises.key), captor.capture())(any())

        captor.getValue match {
          case tp :: tail =>
            tp.removalReason      must be(Some(Schema.OTHER))
            tp.removalReasonOther must be(Some("Some reason"))
        }

      }

      "redirect to the 'Remove trading premises' page" in new Fixture {

        val formRequest = FakeRequest(POST, routes.RemoveAgentPremisesReasonsController.post(1).url)
          .withFormUrlEncodedBody(
            "removalReason"      -> Other.toString,
            "removalReasonOther" -> "Some reason"
          )

        val result = controller.post(1)(formRequest)

        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(
          Some(controllers.tradingpremises.routes.RemoveTradingPremisesController.get(1).url)
        )

      }
    }
  }

}
