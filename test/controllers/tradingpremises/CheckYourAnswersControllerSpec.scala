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
import models.businessmatching.BusinessActivity.{AccountancyServices, BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness}
import models.businessmatching.BusinessMatchingMsbService.{CurrencyExchange, TransmittingMoney}
import models.businessmatching.{BusinessActivities => BusinessMatchingActivities, BusinessMatching, _}
import models.status.SubmissionDecisionApproved
import models.tradingpremises.{Address, TradingPremises, YourTradingPremises}
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers.{OK, contentAsString, status, _}
import play.api.test.{FakeRequest, Injecting}
import services.StatusService
import services.cache.Cache
import utils.AmlsSpec
import utils.tradingpremises.CheckYourAnswersHelper
import views.html.tradingpremises.CheckYourAnswersView

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  implicit val request: FakeRequest.type = FakeRequest
  val userId                             = s"user-${UUID.randomUUID()}"
  val mockDataCacheConnector             = mock[DataCacheConnector]
  val mockCacheMap                       = mock[Cache]
  val statusService                      = mock[StatusService]

  trait Fixture {
    self =>
    val request    = addToken(authRequest)
    lazy val view  = inject[CheckYourAnswersView]
    val controller = new CheckYourAnswersController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      mockDataCacheConnector,
      cc = mockMcc,
      cyaHelper = inject[CheckYourAnswersHelper],
      view = view,
      error = errorView
    )

    when(
      statusService.getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
    ) thenReturn Future.successful(SubmissionDecisionApproved)

    val model = TradingPremises()
  }

  "DetailedAnswersController" when {

    "get is called" must {
      "respond with OK and show the detailed answers page" in new Fixture {
        when(mockDataCacheConnector.fetchAll(any()))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val businessMatchingActivitiesAll = BusinessMatchingActivities(
          Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness)
        )

        val businessMatchingMsbServices = BusinessMatchingMsbServices(Set(TransmittingMoney, CurrencyExchange))

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(
            Some(BusinessMatching(None, Some(businessMatchingActivitiesAll), Some(businessMatchingMsbServices)))
          )

        when(mockCacheMap.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any()))
          .thenReturn(Some(Seq(TradingPremises())))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        contentAsString(result) must include(messages("title.cya"))
        contentAsString(result) must include("/anti-money-laundering/trading-premises/check-your-answers")
      }
    }

    "post is called" must {
      "redirect to YourTradingPremisesController" when {
        "all questions are complete and answers accepted" in new Fixture {

          val ytpModel = YourTradingPremises(
            "foo",
            Address("1", None, None, None, "AA1 1BB", None),
            None,
            Some(LocalDate.of(2010, 10, 10)),
            None
          )

          val emptyCache = Cache.empty

          val newRequest = FakeRequest(POST, routes.CheckYourAnswersController.post(1).url)
            .withFormUrlEncodedBody("hasAccepted" -> "true")

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
            .thenReturn(
              Future.successful(Some(Seq(TradingPremises(yourTradingPremises = Some(ytpModel), hasAccepted = true))))
            )

          when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any(), any())(any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post(1)(newRequest)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.tradingpremises.routes.YourTradingPremisesController.get().url)
          )
        }
      }
    }

  }
}
