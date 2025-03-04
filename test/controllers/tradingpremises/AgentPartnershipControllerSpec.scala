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

import controllers.actions.SuccessfulAuthAction
import forms.tradingpremises.AgentPartnershipFormProvider
import generators.tradingpremises.TradingPremisesGenerator
import models.TradingPremisesSection
import models.tradingpremises.{AgentPartnership, TradingPremises}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import utils.{AmlsSpec, DependencyMocks}
import views.html.tradingpremises.AgentPartnershipView

import scala.concurrent.Future

class AgentPartnershipControllerSpec
    extends AmlsSpec
    with MockitoSugar
    with ScalaFutures
    with TradingPremisesGenerator
    with Injecting {

  trait Fixture extends DependencyMocks { self =>

    val request    = addToken(authRequest)
    lazy val view  = inject[AgentPartnershipView]
    val controller = new AgentPartnershipController(
      mockCacheConnector,
      SuccessfulAuthAction,
      ds = commonDependencies,
      messagesApi,
      cc = mockMcc,
      formProvider = inject[AgentPartnershipFormProvider],
      view = view,
      error = errorView
    )

    mockCacheFetchAll
    mockCacheGetEntry[Seq[TradingPremises]](Some(Seq(tradingPremisesGen.sample.get)), TradingPremises.key)
  }

  "AgentPartnershipController" when {

    "get is called" must {
      "display agent partnership Page" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises()))))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        val title =
          s"${messages("tradingpremises.agentpartnership.title")} - ${messages("summary.tradingpremises")} - ${messages("title.amls")} - ${messages("title.gov")}"

        document.title()                            must be(title)
        document.select("input[type=text]").`val`() must be(empty)
      }

      "display main Summary Page" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises(agentPartnership = Some(AgentPartnership("test")))))))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        val title =
          s"${messages("tradingpremises.agentpartnership.title")} - ${messages("summary.tradingpremises")} - ${messages("title.amls")} - ${messages("title.gov")}"

        document.title()                            must be(title)
        document.select("input[type=text]").`val`() must be("test")
      }
      "respond with NOT_FOUND" when {
        "there is no data at all at that index" in new Fixture {
          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
            .thenReturn(Future.successful(None))

          val result = controller.get(1)(request)

          status(result) must be(NOT_FOUND)
        }
      }
    }

    "post is called" must {
      "respond with NOT_FOUND" when {
        "there is no data at all at that index" in new Fixture {
          val newRequest = FakeRequest(POST, routes.AgentPartnershipController.post(99).url)
            .withFormUrlEncodedBody(
              "agentPartnership" -> "text"
            )

          val result = controller.post(99)(newRequest)
          status(result) must be(NOT_FOUND)
        }
      }
      "respond with SEE_OTHER" when {
        "edit is false and given valid data" in new Fixture {

          val newRequest = FakeRequest(POST, routes.AgentPartnershipController.post(1).url)
            .withFormUrlEncodedBody(
              "agentPartnership" -> "text"
            )

          when(controller.dataCacheConnector.save(any(), any(), any())(any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(1)(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.ConfirmAddressController.get(1).url))
        }

        "edit is true and given valid data" in new Fixture {

          val newRequest = FakeRequest(POST, routes.AgentPartnershipController.post(1, true).url)
            .withFormUrlEncodedBody(
              "agentPartnership" -> "text"
            )

          when(controller.dataCacheConnector.save(any(), any(), any())(any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(1, true)(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.CheckYourAnswersController.get(1).url))

        }
      }

      "respond with BAD_REQUEST" when {
        "given invalid data" in new Fixture {

          val newRequest = FakeRequest(POST, routes.AgentPartnershipController.post(1).url)
            .withFormUrlEncodedBody(
              "agentPartnership" -> ""
            )

          val result = controller.post(1)(newRequest)
          status(result) must be(BAD_REQUEST)

        }
      }
      "set the hasChanged flag to true" in new Fixture {

        val newRequest = FakeRequest(POST, routes.AgentPartnershipController.post(1).url)
          .withFormUrlEncodedBody("agentPartnership" -> "text")
        when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
          .thenReturn(Some(Seq(TradingPremisesSection.tradingPremisesWithHasChangedFalse, TradingPremises())))

        when(controller.dataCacheConnector.save(any(), any(), any())(any()))
          .thenReturn(Future.successful(mockCacheMap))

        val result = controller.post(1)(newRequest)

        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1, false).url))

        verify(controller.dataCacheConnector).save[Seq[TradingPremises]](
          any(),
          any(),
          meq(
            Seq(
              TradingPremisesSection.tradingPremisesWithHasChangedFalse.copy(
                hasChanged = true,
                agentPartnership = Some(AgentPartnership("text")),
                agentName = None,
                agentCompanyDetails = None
              ),
              TradingPremises()
            )
          )
        )(any())
      }
    }

  }
}
