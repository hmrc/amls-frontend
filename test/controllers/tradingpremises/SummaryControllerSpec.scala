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

import java.util.UUID

import connectors.DataCacheConnector
import models.businessmatching.{BusinessActivities => BusinessMatchingActivities, _}
import models.businessmatching.{AccountancyServices, BillPaymentServices, BusinessMatching, EstateAgentBusinessService}
import models.status.{SubmissionDecisionApproved, SubmissionReady, SubmissionReadyForReview}
import models.tradingpremises.{Address, RegisteringAgentPremises, TradingPremises, YourTradingPremises}
import org.joda.time.LocalDate
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.AuthorisedFixture
import org.mockito.Matchers.{eq => meq}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class SummaryControllerSpec extends GenericTestHelper with MockitoSugar {

  implicit val request = FakeRequest
  val userId = s"user-${UUID.randomUUID()}"
  val mockDataCacheConnector = mock[DataCacheConnector]
  val mockCacheMap = mock[CacheMap]

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val summaryController = new SummaryController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
      override val statusService = mock[StatusService]
    }

    when(summaryController.statusService.getStatus(any(), any(), any())) thenReturn Future.successful(SubmissionDecisionApproved)

    val model = TradingPremises()

    val models = Seq(TradingPremises())
  }

  "SummaryController" must {

    "load the summary page when the model is present" in new Fixture {
      val businessMatchingActivitiesAll = BusinessMatchingActivities(
        Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))

      when(mockDataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))
      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(model)))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

      val result = summaryController.get()(request)
      status(result) must be(OK)
    }

    "redirect to the main amls summary page when section data is unavailable" in new Fixture {

      val businessMatchingActivitiesAll = BusinessMatchingActivities(
        Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))

      when(mockDataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))
      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(None)
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

      val result = summaryController.get()(request)
      redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get.url))
      status(result) must be(SEE_OTHER)
    }

    "for an individual display the trading premises summary page for individual" in new Fixture {

      when(mockDataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      val businessMatchingActivitiesAll = BusinessMatchingActivities(
        Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness))

      val businessMatchingMsbServices = MsbServices(
        Set(TransmittingMoney, CurrencyExchange))

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll), Some(businessMatchingMsbServices))))

      when(mockCacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))
        (any())).thenReturn(Some(Seq(TradingPremises())))

      val result = summaryController.getIndividual(1)(request)

      status(result) must be(OK)
    }


    "for an individual redirect to the trading premises summary summary if data is not present" in new Fixture {
      when(mockCacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))
        (any())).thenReturn(None)
      val result = summaryController.getIndividual(1)(request)
      status(result) must be(NOT_FOUND)
    }

    "direct to your answers when the model is present" in new Fixture {
      val businessMatchingActivitiesAll = BusinessMatchingActivities(
        Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness))

      when(mockDataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))
      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(model)))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

      val result = summaryController.answers()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("summary.youranswers.title"))
    }

  }

  "post is called" must {
    "redirect to the progress page" when {

      "all questions are complete" in new Fixture {

        val ytpModel = YourTradingPremises("foo", Address("1","2",None,None,"AA1 1BB",None), None, Some(new LocalDate(2010, 10, 10)), None)
        val ytp = Some(ytpModel)

        val emptyCache = CacheMap("", Map.empty)

        val newRequest = request.withFormUrlEncodedBody( "hasAccepted" -> "true")

        when(summaryController.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises(yourTradingPremises =  Some(ytpModel), hasAccepted = true)))))

        when(summaryController.dataCacheConnector.save[Seq[TradingPremises]](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = summaryController.post()(newRequest)

        status(result) must be(SEE_OTHER)
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
          controllers.tradingpremises.routes.RemoveAgentPremisesReasonsController.get(1).url)

      }

      "the trading premises is an agent but the status is an amendment" in {

        val tradingPremises = TradingPremises(Some(RegisteringAgentPremises(true)), lineId = Some(1234))

        tradingPremises.removeUrl(1, status = SubmissionReadyForReview) must be(
          controllers.tradingpremises.routes.RemoveTradingPremisesController.get(1).url)

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
