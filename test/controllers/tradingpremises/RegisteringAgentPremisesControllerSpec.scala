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
import forms.tradingpremises.RegisteringAgentPremisesFormProvider
import models.TradingPremisesSection
import models.businessmatching.BusinessActivity._
import models.businessmatching.{BusinessActivities => BusinessMatchingActivities, _}
import models.tradingpremises.{RegisteringAgentPremises, TradingPremises}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.AmlsSpec
import views.html.tradingpremises.RegisteringAgentPremisesView

import scala.concurrent.Future

class RegisteringAgentPremisesControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = inject[RegisteringAgentPremisesView]
    val controller = new RegisteringAgentPremisesController(
      mock[DataCacheConnector],
      SuccessfulAuthAction,
      ds = commonDependencies,
      messagesApi,
      cc = mockMcc,
      formProvider = inject[RegisteringAgentPremisesFormProvider],
      view = view,
      error = errorView
    )
  }

  val emptyCache = Cache.empty

  val mockCacheMap = mock[Cache]

  "RegisteringAgentPremisesController" when {

    "get is called" when {
      "edit is false" when {
        "it is not an MSB" must {
          "redirect to Trading Premises Details form" in new Fixture {
            val model                         = TradingPremises(
              registeringAgentPremises = Some(
                RegisteringAgentPremises(true)
              )
            )
            val businessMatchingActivitiesAll =
              BusinessMatchingActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
            when(controller.dataCacheConnector.fetchAll(any()))
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
              Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness)
            )
            val model                         = TradingPremises()
            when(controller.dataCacheConnector.fetchAll(any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))
            when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
              .thenReturn(Some(Seq(model)))
            when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

            val result = controller.get(1)(request)
            status(result) must be(OK)

          }

          "load Yes when mongoCache returns true" in new Fixture {

            val model                         = TradingPremises(
              registeringAgentPremises = Some(
                RegisteringAgentPremises(true)
              )
            )
            val businessMatchingActivitiesAll = BusinessMatchingActivities(
              Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness)
            )
            when(controller.dataCacheConnector.fetchAll(any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))
            when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
              .thenReturn(Some(Seq(model)))
            when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

            val result = controller.get(1)(request)
            status(result) must be(OK)

            val htmlValue = Jsoup.parse(contentAsString(result))
            htmlValue.getElementById("agentPremises").hasAttr("checked") mustBe true

          }
          "load No when mongoCache returns false" in new Fixture {

            val model                         = TradingPremises(
              registeringAgentPremises = Some(
                RegisteringAgentPremises(false)
              )
            )
            val businessMatchingActivitiesAll = BusinessMatchingActivities(
              Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness)
            )
            when(controller.dataCacheConnector.fetchAll(any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))
            when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
              .thenReturn(Some(Seq(model)))
            when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

            val result = controller.get(1)(request)
            status(result) must be(OK)

            val htmlValue = Jsoup.parse(contentAsString(result))
            htmlValue.getElementById("agentPremises-2").hasAttr("checked") mustBe true

          }

          "respond with NOT_FOUND when there is no data" in new Fixture {
            val businessMatchingActivitiesAll = BusinessMatchingActivities(
              Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness)
            )
            when(controller.dataCacheConnector.fetchAll(any()))
              .thenReturn(Future.successful(None))
            when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
              .thenReturn(None)
            when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

            val result = controller.get(1)(request)
            status(result) must be(NOT_FOUND)
          }

          "respond with NOT_FOUND when there is no data at all at the given index" in new Fixture {
            val model                         = TradingPremises(
              registeringAgentPremises = Some(
                RegisteringAgentPremises(true)
              )
            )
            val businessMatchingActivitiesAll = BusinessMatchingActivities(
              Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness)
            )
            when(controller.dataCacheConnector.fetchAll(any()))
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
        val newRequest = FakeRequest(POST, routes.RegisteringAgentPremisesController.post(1).url)
          .withFormUrlEncodedBody("" -> "")
        val result     = controller.post(1)(newRequest)
        status(result) must be(BAD_REQUEST)

      }

      "redirect to the details page on submitting false and edit true" in new Fixture {

        val model = TradingPremises(
          registeringAgentPremises = Some(
            RegisteringAgentPremises(true)
          )
        )

        val newRequest = FakeRequest(POST, routes.RegisteringAgentPremisesController.post(1, true).url)
          .withFormUrlEncodedBody(
            "agentPremises" -> "false"
          )

        when(controller.dataCacheConnector.fetchAll(any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))
        when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
          .thenReturn(Some(Seq(model)))

        when(controller.dataCacheConnector.save(any(), any(), any())(any()))
          .thenReturn(Future.successful(mockCacheMap))

        val result = controller.post(1, edit = true)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.CheckYourAnswersController.get(1).url)
      }

      "redirect to the Trading Premises details page on submitting false and edit false" in new Fixture {

        val model = TradingPremises(
          registeringAgentPremises = Some(
            RegisteringAgentPremises(true)
          )
        )

        val newRequest = FakeRequest(POST, routes.RegisteringAgentPremisesController.post(1).url)
          .withFormUrlEncodedBody(
            "agentPremises" -> "false"
          )

        when(controller.dataCacheConnector.fetchAll(any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))
        when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
          .thenReturn(Some(Seq(model, model)))

        when(controller.dataCacheConnector.save(any(), any(), any())(any()))
          .thenReturn(Future.successful(mockCacheMap))

        val result = controller.post(1, edit = false)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.WhereAreTradingPremisesController.get(1).url)
      }
      "redirect to the 'what is your agent's business structure?' page on submitting true" in new Fixture {

        val model = TradingPremises(
          registeringAgentPremises = Some(
            RegisteringAgentPremises(true)
          )
        )

        val newRequest = FakeRequest(POST, routes.RegisteringAgentPremisesController.post(1).url)
          .withFormUrlEncodedBody(
            "agentPremises" -> "true"
          )

        when(controller.dataCacheConnector.fetchAll(any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))
        when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
          .thenReturn(Some(Seq(model)))

        when(controller.dataCacheConnector.save(any(), any(), any())(any()))
          .thenReturn(Future.successful(mockCacheMap))

        val result = controller.post(1, edit = false)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.BusinessStructureController.get(1, false).url)
      }

      "respond with NOT_FOUND" when {
        "the given index is out of bounds" in new Fixture {

          val newRequest = FakeRequest(POST, routes.RegisteringAgentPremisesController.post(1).url)
            .withFormUrlEncodedBody(
              "agentPremises" -> "true"
            )
          when(controller.dataCacheConnector.fetchAll(any()))
            .thenReturn(Future.successful(Some(mockCacheMap)))
          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(TradingPremises(Some(RegisteringAgentPremises(true)), None, None, None))))

          val result = controller.post(10, false)(newRequest)

          status(result) must be(NOT_FOUND)

        }
      }

      "set the hasChanged flag to true" in new Fixture {

        val newRequest = FakeRequest(POST, routes.RegisteringAgentPremisesController.post(1).url)
          .withFormUrlEncodedBody(
            "agentPremises" -> "false"
          )
        when(controller.dataCacheConnector.fetchAll(any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
          .thenReturn(Some(Seq(TradingPremisesSection.tradingPremisesWithHasChangedFalse, TradingPremises())))

        when(controller.dataCacheConnector.save[TradingPremises](any(), any(), any())(any()))
          .thenReturn(Future.successful(emptyCache))

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
                registeringAgentPremises = Some(RegisteringAgentPremises(false)),
                agentName = None,
                businessStructure = None,
                agentCompanyDetails = None,
                agentPartnership = None
              ),
              TradingPremises()
            )
          )
        )(any())
      }

    }
  }
}
