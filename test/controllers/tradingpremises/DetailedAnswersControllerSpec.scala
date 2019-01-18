/*
 * Copyright 2019 HM Revenue & Customs
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

import models.tradingpremises.TradingPremises
import play.api.test.Helpers.{OK, contentAsString, status}
import connectors.DataCacheConnector
import models.businessmatching.{AccountancyServices, BillPaymentServices, BusinessMatching, EstateAgentBusinessService, BusinessActivities => BusinessMatchingActivities, _}
import models.status.SubmissionDecisionApproved
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{AmlsSpec, AuthorisedFixture}

import scala.concurrent.Future


class DetailedAnswersControllerSpec extends AmlsSpec with MockitoSugar {

  implicit val request = FakeRequest
  val userId = s"user-${UUID.randomUUID()}"
  val mockDataCacheConnector = mock[DataCacheConnector]
  val mockCacheMap = mock[CacheMap]
  val statusService = mock[StatusService]

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val controller = new DetailedAnswersController(self.authConnector, mock[DataCacheConnector])

    when(statusService.getStatus(any(), any(), any())) thenReturn Future.successful(SubmissionDecisionApproved)

    val model = TradingPremises()
  }

  "DetailedAnswersController" when {

    "get is called" must {
      "respond with OK and show the detailed answers page" in new Fixture {
        when(mockDataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        val businessMatchingActivitiesAll = BusinessMatchingActivities(
          Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness))

        val businessMatchingMsbServices = BusinessMatchingMsbServices(
          Set(TransmittingMoney, CurrencyExchange))

        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll), Some(businessMatchingMsbServices))))

        when(mockCacheMap.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))
          (any())).thenReturn(Some(Seq(TradingPremises())))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        contentAsString(result) must include(Messages("title.cya"))
        contentAsString(result) must include("/anti-money-laundering/trading-premises/check-your-answers")
      }
    }

    "post is called" must {
      "redirect to YourTradingPremisesController" in new Fixture {

      }
    }

  }
}