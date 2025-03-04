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
import models.businessmatching.BusinessActivity._
import models.businessmatching.{BusinessActivities => BusinessMatchingActivities, BusinessMatching}
import models.status.{SubmissionDecisionApproved, SubmissionReady, SubmissionReadyForReview}
import models.tradingpremises._
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.StatusService
import services.cache.Cache
import utils.{AmlsSpec, DependencyMocks}
import views.html.tradingpremises.YourTradingPremisesView

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Future

class YourTradingPremisesControllerSpec
    extends AmlsSpec
    with MockitoSugar
    with generators.tradingpremises.TradingPremisesGenerator {

  implicit val request: FakeRequest.type = FakeRequest
  val userId                             = s"user-${UUID.randomUUID()}"
  val mockDataCacheConnector             = mock[DataCacheConnector]
  val mockStatusService                  = mock[StatusService]
  val mockYtp                            = mock[TradingPremises]

  trait Fixture extends DependencyMocks {
    self =>
    val request       = addToken(authRequest)
    lazy val view     = app.injector.instanceOf[YourTradingPremisesView]
    val ytpController = new YourTradingPremisesController(
      mockDataCacheConnector,
      mockStatusService,
      SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      view = view,
      error = errorView
    )

    when(
      ytpController.statusService
        .getStatus(any[Option[String]](), any[(String, String)](), any[String]())(any(), any(), any())
    ) thenReturn Future.successful(SubmissionDecisionApproved)

    val model  = TradingPremises()
    val models = Seq(TradingPremises())
  }

  "YourTradingPremisesController" must {

    "load the summary page when the model is present" in new Fixture {
      val businessMatchingActivitiesAll =
        BusinessMatchingActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))

      when(mockDataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))
      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(model)))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

      val result = ytpController.get()(request)
      status(result) must be(OK)
    }

    "redirect to the main amls summary page when section data is unavailable" in new Fixture {

      val businessMatchingActivitiesAll =
        BusinessMatchingActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))

      when(mockDataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))
      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(None)
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

      val result = ytpController.get()(request)
      redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get().url))
      status(result)           must be(SEE_OTHER)
    }

    "for a complete individual display the trading premises check your answers page" in new Fixture {

      when(mockDataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val businessMatchingActivities =
        BusinessMatchingActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))

      val ytp = tradingPremisesGen.sample.get.copy(whatDoesYourBusinessDoAtThisAddress =
        Some(WhatDoesYourBusinessDo(Set(AccountancyServices, HighValueDealing, TelephonePaymentService), None))
      )

      when(mockCacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))(any())).thenReturn(Some(Seq(ytp)))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivities))))

      val result = ytpController.getIndividual(1, true)(request)

      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.CheckYourAnswersController.get(1).url))
    }

    "for an individual redirect to the trading premises summary summary if data is not present" in new Fixture {

      when(mockDataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(mockCacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))(any())).thenReturn(None)

      val businessMatchingActivities =
        BusinessMatchingActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivities))))

      val result = ytpController.getIndividual(1)(request)

      status(result) must be(NOT_FOUND)

    }

    "direct to your answers when the model is present" in new Fixture {
      val businessMatchingActivitiesAll = BusinessMatchingActivities(
        Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness)
      )

      when(mockDataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))
      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(model)))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

      val result = ytpController.answers()(request)
      status(result)          must be(OK)
      contentAsString(result) must include(Messages("tradingpremises.yourpremises.title"))
    }

  }

  "post is called" must {
    "redirect to the progress page" when {

      "all questions are complete" in new Fixture {

        val ytpModel = YourTradingPremises(
          "foo",
          Address("1", None, None, None, "AA1 1BB", None),
          None,
          Some(LocalDate.of(2010, 10, 10)),
          None
        )

        val emptyCache = Cache.empty

        val newRequest = requestWithUrlEncodedBody("hasAccepted" -> "true")

        when(ytpController.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
          .thenReturn(
            Future.successful(Some(Seq(TradingPremises(yourTradingPremises = Some(ytpModel), hasAccepted = true))))
          )

        when(ytpController.dataCacheConnector.save[Seq[TradingPremises]](any(), any(), any())(any()))
          .thenReturn(Future.successful(emptyCache))

        val result = ytpController.post()(newRequest)

        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get().url))
      }

    }
  }

  "ModelHelpers" must {

    import controllers.tradingpremises.ModelHelpers._

    "return the correct removal url" when {

      "the trading premises is an Agent trading premises" in {

        val tradingPremises = TradingPremises(Some(RegisteringAgentPremises(true)), lineId = Some(1234))

        tradingPremises.removeUrl(1, status = SubmissionDecisionApproved) must be(
          controllers.tradingpremises.routes.RemoveAgentPremisesReasonsController.get(1).url
        )

      }

      "the trading premises is an agent but the status is an amendment" in {

        val tradingPremises = TradingPremises(Some(RegisteringAgentPremises(true)), lineId = Some(1234))

        tradingPremises.removeUrl(1, status = SubmissionReadyForReview) must be(
          controllers.tradingpremises.routes.RemoveTradingPremisesController.get(1).url
        )

      }

      "the trading premises is not an Agent trading premises" in {
        val tradingPremises = TradingPremises(Some(RegisteringAgentPremises(false)))

        tradingPremises.removeUrl(1, status = SubmissionDecisionApproved) must be(
          controllers.tradingpremises.routes.RemoveTradingPremisesController.get(1).url
        )
      }

      "the status is a new submission" in {

        val tradingPremises = TradingPremises(Some(RegisteringAgentPremises(true)))

        tradingPremises.removeUrl(1, status = SubmissionReady) must be(
          controllers.tradingpremises.routes.RemoveTradingPremisesController.get(1).url
        )

      }

      "the status is a variation but the trading premises has no line Id" in {

        val tradingPremises = TradingPremises(Some(RegisteringAgentPremises(true)), lineId = None)

        tradingPremises.removeUrl(1, status = SubmissionDecisionApproved) must be(
          controllers.tradingpremises.routes.RemoveTradingPremisesController.get(1).url
        )
      }
    }
  }
}
