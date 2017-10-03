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

package controllers.businessmatching.updateservice

import cats.implicits._
import cats.data.OptionT
import connectors.DataCacheConnector
import models.businessmatching._
import models.status.{NotCompleted, SubmissionDecisionApproved}
import models.tradingpremises.{Address, TradingPremises, YourTradingPremises}
import org.joda.time.LocalDate
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.StatusService
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class WhichTradingPremisesControllerSpec extends GenericTestHelper {

  sealed trait Fixture extends AuthorisedFixture with DependencyMocks {

    self =>
    val request = addToken(authRequest)

    val ytp = YourTradingPremises(
      "name1",
      Address(
        "add1Line1",
        "add2Line2",
        None,
        None,
        "ps11de"
      ),
      Some(true),
      Some(new LocalDate(1990, 2, 24))
    )

    val tradingPremises = Seq(
      TradingPremises(
        yourTradingPremises = Some(ytp)
      )
    )

    val mockBusinessMatchingService = mock[BusinessMatchingService]

    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val authContext: AuthContext = mock[AuthContext]
    implicit val ec: ExecutionContext = mock[ExecutionContext]

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[BusinessMatchingService].to(mockBusinessMatchingService))
      .overrides(bind[StatusService].to(mockStatusService))
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))
      .build()

    val controller = app.injector.instanceOf[WhichTradingPremisesController]

  }

  "WhichTradingPremisesController" when {

    "get is called" must {
      "return OK with trading_premises view" in new Fixture {

        mockApplicationStatus(SubmissionDecisionApproved)
        mockCacheFetch[Seq[TradingPremises]](Some(tradingPremises), Some(TradingPremises.key))

        when {
          controller.businessMatchingService.getAdditionalBusinessActivities(any(),any(),any())
        } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(HighValueDealing))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(
          Messages(
            "businessmatching.updateservice.whichtradingpremises.header",
            Messages(s"businessmatching.registerservices.servicename.lbl.${BusinessActivities.getValue(HighValueDealing)}")
          ))
      }
      "return NOT_FOUND" when {
        "pre-submission" in new Fixture {

          mockApplicationStatus(NotCompleted)
          mockCacheFetch[Seq[TradingPremises]](Some(tradingPremises), Some(TradingPremises.key))

          when {
            controller.businessMatchingService.getAdditionalBusinessActivities(any(),any(),any())
          } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(HighValueDealing))

          val result = controller.get()(request)
          status(result) must be(NOT_FOUND)

        }
      }
      "return INTERNAL_SERVER_ERROR" when {
        "trading premises cannot be retrieved" in new Fixture {

          mockApplicationStatus(SubmissionDecisionApproved)
          mockCacheFetch[Seq[TradingPremises]](None, Some(TradingPremises.key))

          when {
            controller.businessMatchingService.getAdditionalBusinessActivities(any(),any(),any())
          } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(HighValueDealing))

          val result = controller.get()(request)
          status(result) must be(INTERNAL_SERVER_ERROR)

        }
        "activities cannot be retrieved" in new Fixture {

          mockApplicationStatus(SubmissionDecisionApproved)
          mockCacheFetch[Seq[TradingPremises]](Some(tradingPremises), Some(TradingPremises.key))

          when {
            controller.businessMatchingService.getAdditionalBusinessActivities(any(),any(),any())
          } thenReturn OptionT.none[Future, Set[BusinessActivity]]

          val result = controller.get()(request)
          status(result) must be(INTERNAL_SERVER_ERROR)

        }
      }
    }

  }

}