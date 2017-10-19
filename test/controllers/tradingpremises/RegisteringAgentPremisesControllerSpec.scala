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
import models.TradingPremisesSection
import models.tradingpremises.{RegisteringAgentPremises, TradingPremises}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.AuthorisedFixture
import models.businessmatching.{BusinessActivities => BusinessMatchingActivities, _}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class RegisteringAgentPremisesControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)
    val controller = new RegisteringAgentPremisesController (mock[DataCacheConnector],
      self.authConnector, messagesApi)
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
            when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
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
            when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
              .thenReturn(Future.successful(Some(mockCacheMap)))
            when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
              .thenReturn(Some(Seq(model)))
            when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

            val result = controller.get(1)(request)
            status(result) must be(OK)

          }

          "load Yes when save4later returns true" in new Fixture {

            val model = TradingPremises(
              registeringAgentPremises = Some(
                RegisteringAgentPremises(true)
              )
            )
            val businessMatchingActivitiesAll = BusinessMatchingActivities(
              Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness))
            when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
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
          "load No when save4later returns false" in new Fixture {

            val model = TradingPremises(
              registeringAgentPremises = Some(
                RegisteringAgentPremises(false)
              )
            )
            val businessMatchingActivitiesAll = BusinessMatchingActivities(
              Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness))
            when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
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
            when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
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
            when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
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
        val newRequest = request.withFormUrlEncodedBody()
        val result = controller.post(1)(newRequest)
        status(result) must be(BAD_REQUEST)

      }

      "redirect to the Trading Premises details page on submitting false and edit true" in new Fixture {

        val model = TradingPremises(
          registeringAgentPremises = Some(
            RegisteringAgentPremises(true)
          )
        )

        val newRequest = request.withFormUrlEncodedBody(
          "agentPremises" -> "false"
        )

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))
        when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
          .thenReturn(Some(Seq(model)))

        val result = controller.post(1,edit = true)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.SummaryController.getIndividual(1).url)
      }

      "redirect to the Trading Premises details page on submitting false and edit false" in new Fixture {

        val model = TradingPremises(
          registeringAgentPremises = Some(
            RegisteringAgentPremises(true)
          )
        )

        val newRequest = request.withFormUrlEncodedBody(
          "agentPremises" -> "false"
        )

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))
        when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
          .thenReturn(Some(Seq(model, model)))

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

        val newRequest = request.withFormUrlEncodedBody(
          "agentPremises" -> "true"
        )

        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))
        when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
          .thenReturn(Some(Seq(model)))

        val result = controller.post(1,edit = false)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.BusinessStructureController.get(1,false).url)
      }

      "respond with NOT_FOUND" when {
        "the given index is out of bounds" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "agentPremises" -> "true"
          )
          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))
          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(TradingPremises(Some(RegisteringAgentPremises(true)), None, None, None))))

          val result = controller.post(10, false)(newRequest)

          status(result) must be(NOT_FOUND)

        }
      }

      "set the hasChanged flag to true" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "agentPremises" -> "false"
        )
        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
          .thenReturn(Some(Seq(TradingPremisesSection.tradingPremisesWithHasChangedFalse, TradingPremises())))

        when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(1)(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1, false).url))

        verify(controller.dataCacheConnector).save[Seq[TradingPremises]](
          any(),
          meq(Seq(TradingPremisesSection.tradingPremisesWithHasChangedFalse.copy(
            hasChanged = true,
            registeringAgentPremises = Some(RegisteringAgentPremises(false)),
            agentName=None,
            businessStructure=None,
            agentCompanyDetails=None,
            agentPartnership=None
          ), TradingPremises())))(any(), any(), any())
      }

    }
  }
}
