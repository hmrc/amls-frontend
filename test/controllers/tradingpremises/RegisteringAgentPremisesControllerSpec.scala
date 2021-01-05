/*
 * Copyright 2021 HM Revenue & Customs
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
import models.TradingPremisesSection
import models.businessmatching.{BusinessActivities => BusinessMatchingActivities, _}
import models.tradingpremises.{RegisteringAgentPremises, TradingPremises}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, DependencyMocks}
import views.html.tradingpremises.registering_agent_premises

import scala.concurrent.Future

class RegisteringAgentPremisesControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture  {
    self => val request = addToken(authRequest)
    lazy val view = app.injector.instanceOf[registering_agent_premises]
    val controller = new RegisteringAgentPremisesController (
      mock[DataCacheConnector],
      SuccessfulAuthAction, ds = commonDependencies,
      messagesApi,
      cc = mockMcc,
      registering_agent_premises = view,
      error = errorView)
  }

  val emptyCache = CacheMap("", Map.empty)

  val mockCacheMap = mock[CacheMap]

  "RegisteringAgentPremisesController" when {

    "get is called" when {
      "edit is false" when {
        "it is not an MSB" must {
          "redirect to Trading Premises Details form" in new Fixture {
            val model = TradingPremises(
              registeringAgentPremises = Some(
                RegisteringAgentPremises(true)
              )
            )
            val businessMatchingActivitiesAll = BusinessMatchingActivities(
              Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
            when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
              .thenReturn(Future.successful(Some(mockCacheMap)))
            when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
              .thenReturn(Some(Seq(model)))
            when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

            val result = controller.get(1)(request)
            status(result) must be(SEE_OTHER)

            redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1).url))

          }
        }

        "it is an MSB" must {
          "load the Register Agent Premises page" in new Fixture {

            val businessMatchingActivitiesAll = BusinessMatchingActivities(
              Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness))
            val model = TradingPremises()
            when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
              .thenReturn(Future.successful(Some(mockCacheMap)))
            when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
              .thenReturn(Some(Seq(model)))
            when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

            val result = controller.get(1)(request)
            status(result) must be(OK)

          }

          "load Yes when mongoCache returns true" in new Fixture {

            val model = TradingPremises(
              registeringAgentPremises = Some(
                RegisteringAgentPremises(true)
              )
            )
            val businessMatchingActivitiesAll = BusinessMatchingActivities(
              Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness))
            when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
              .thenReturn(Future.successful(Some(mockCacheMap)))
            when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
              .thenReturn(Some(Seq(model)))
            when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

            val result = controller.get(1)(request)
            status(result) must be(OK)

            val htmlValue = Jsoup.parse(contentAsString(result))
            htmlValue.getElementById("agentPremises-true").attr("checked") mustBe "checked"

          }
          "load No when mongoCache returns false" in new Fixture {

            val model = TradingPremises(
              registeringAgentPremises = Some(
                RegisteringAgentPremises(false)
              )
            )
            val businessMatchingActivitiesAll = BusinessMatchingActivities(
              Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness))
            when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
              .thenReturn(Future.successful(Some(mockCacheMap)))
            when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
              .thenReturn(Some(Seq(model)))
            when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

            val result = controller.get(1)(request)
            status(result) must be(OK)

            val htmlValue = Jsoup.parse(contentAsString(result))
            htmlValue.getElementById("agentPremises-false").attr("checked") mustBe "checked"

          }

          "respond with NOT_FOUND when there is no data" in new Fixture {
            val model = TradingPremises(
              registeringAgentPremises = Some(
                RegisteringAgentPremises(true)
              )
            )
            val businessMatchingActivitiesAll = BusinessMatchingActivities(
              Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness))
            when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
              .thenReturn(Future.successful(None))
            when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
              .thenReturn(None)
            when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

            val result = controller.get(1)(request)
            status(result) must be(NOT_FOUND)
          }

          "respond with NOT_FOUND when there is no data at all at the given index" in new Fixture {
            val model = TradingPremises(
              registeringAgentPremises = Some(
                RegisteringAgentPremises(true)
              )
            )
            val businessMatchingActivitiesAll = BusinessMatchingActivities(
              Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness))
            when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
              .thenReturn(Future.successful(None))
            when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
              .thenReturn(Some(Seq(model)))
            when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

            val result = controller.get(1)(request)
            status(result) must be(NOT_FOUND)
          }
        }
      }
    }

    "post is called" must {

      "on post invalid data show error" in new Fixture {
        val newRequest = requestWithUrlEncodedBody("" -> "")
        val result = controller.post(1)(newRequest)
        status(result) must be(BAD_REQUEST)

      }

      "redirect to the details page on submitting false and edit true" in new Fixture {

        val model = TradingPremises(
          registeringAgentPremises = Some(
            RegisteringAgentPremises(true)
          )
        )

        val newRequest = requestWithUrlEncodedBody(
          "agentPremises" -> "false"
        )

        when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(mockCacheMap)))
        when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
          .thenReturn(Some(Seq(model)))

        when(controller.dataCacheConnector.save(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(mockCacheMap))

        val result = controller.post(1,edit = true)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.DetailedAnswersController.get(1).url)
      }

      "redirect to the Trading Premises details page on submitting false and edit false" in new Fixture {

        val model = TradingPremises(
          registeringAgentPremises = Some(
            RegisteringAgentPremises(true)
          )
        )

        val newRequest = requestWithUrlEncodedBody(
          "agentPremises" -> "false"
        )

        when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(mockCacheMap)))
        when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
          .thenReturn(Some(Seq(model, model)))

        when(controller.dataCacheConnector.save(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(mockCacheMap))

        val result = controller.post(1,edit = false)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.WhereAreTradingPremisesController.get(1).url)
      }
      "redirect to the 'what is your agent's business structure?' page on submitting true" in new Fixture {

        val model = TradingPremises(
          registeringAgentPremises = Some(
            RegisteringAgentPremises(true)
          )
        )

        val newRequest = requestWithUrlEncodedBody(
          "agentPremises" -> "true"
        )

        when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(mockCacheMap)))
        when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
          .thenReturn(Some(Seq(model)))

        when(controller.dataCacheConnector.save(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(mockCacheMap))

        val result = controller.post(1,edit = false)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.BusinessStructureController.get(1,false).url)
      }

      "respond with NOT_FOUND" when {
        "the given index is out of bounds" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "agentPremises" -> "true"
          )
          when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
            .thenReturn(Future.successful(Some(mockCacheMap)))
          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(TradingPremises(Some(RegisteringAgentPremises(true)), None, None, None))))

          val result = controller.post(10, false)(newRequest)

          status(result) must be(NOT_FOUND)

        }
      }

      "set the hasChanged flag to true" in new Fixture {

        val newRequest = requestWithUrlEncodedBody(
          "agentPremises" -> "false"
        )
        when(controller.dataCacheConnector.fetchAll(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
          .thenReturn(Some(Seq(TradingPremisesSection.tradingPremisesWithHasChangedFalse, TradingPremises())))

        when(controller.dataCacheConnector.save[TradingPremises](any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(1)(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1, false).url))

        verify(controller.dataCacheConnector).save[Seq[TradingPremises]](
          any(), any(),
          meq(Seq(TradingPremisesSection.tradingPremisesWithHasChangedFalse.copy(
            hasChanged = true,
            registeringAgentPremises = Some(RegisteringAgentPremises(false)),
            agentName=None,
            businessStructure=None,
            agentCompanyDetails=None,
            agentPartnership=None
          ), TradingPremises())))(any(), any())
      }

    }
  }
}
