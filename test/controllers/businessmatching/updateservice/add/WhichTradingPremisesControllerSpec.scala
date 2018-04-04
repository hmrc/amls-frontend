/*
 * Copyright 2018 HM Revenue & Customs
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

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.businessmatching.updateservice.add.WhichTradingPremisesController
import models.DateOfChange
import models.businessmatching._
import models.businessmatching.updateservice.{NewActivitiesAtTradingPremisesNo, UpdateService}
import models.status.{NotCompleted, SubmissionDecisionApproved}
import models.tradingpremises.{Address, TradingPremises, WhatDoesYourBusinessDo, YourTradingPremises}
import org.joda.time.LocalDate
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.PrivateMethodTester
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.StatusService
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper, StatusConstants}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier

class WhichTradingPremisesControllerSpec extends GenericTestHelper with PrivateMethodTester {

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

    val activities = WhatDoesYourBusinessDo(Set(BillPaymentServices), Some(DateOfChange(new LocalDate(2012, 10, 19))))

    val tradingPremises = Seq(
      TradingPremises(
        yourTradingPremises = Some(ytp),
        whatDoesYourBusinessDoAtThisAddress = Some(activities)
      ),
      TradingPremises(
        yourTradingPremises = Some(ytp.copy("name2", Address("add2Line1", "add2Line2", None, None, "ps22de"))),
        whatDoesYourBusinessDoAtThisAddress = Some(activities),
        status = Some(StatusConstants.Deleted)
      ),
      TradingPremises(
        yourTradingPremises = Some(ytp.copy("name3", Address("add3Line1", "add3Line2", None, None, "ps33de"))),
        whatDoesYourBusinessDoAtThisAddress = Some(activities)
      ),
      TradingPremises(
        yourTradingPremises = Some(ytp.copy("name4", Address("add4Line1", "add4Line2", None, None, "ps44de"))),
        whatDoesYourBusinessDoAtThisAddress = Some(activities)
      ),
      TradingPremises(
        yourTradingPremises = Some(ytp.copy("name5", Address("add5Line1", "add5Line2", None, None, "ps55de"))),
        whatDoesYourBusinessDoAtThisAddress = Some(activities)
      )
    )

    mockCacheSave[Seq[TradingPremises]]
    mockCacheSave[UpdateService]

    mockCacheFetch[UpdateService](Some(
      UpdateService(
        Some(NewActivitiesAtTradingPremisesNo)
      )
    ))

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

  "get" must {
    "return OK with trading_premises view" in new Fixture {

      mockApplicationStatus(SubmissionDecisionApproved)
      mockCacheFetch[Seq[TradingPremises]](Some(tradingPremises), Some(TradingPremises.key))

      when {
        controller.businessMatchingService.getAdditionalBusinessActivities(any(), any(), any())
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
          controller.businessMatchingService.getAdditionalBusinessActivities(any(), any(), any())
        } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(HighValueDealing))

        val result = controller.get()(request)
        status(result) must be(NOT_FOUND)

      }
    }
    "return INTERNAL_SERVER_ERROR" when {
      "activities cannot be retrieved" in new Fixture {

        mockApplicationStatus(SubmissionDecisionApproved)
        mockCacheFetch[Seq[TradingPremises]](Some(tradingPremises), Some(TradingPremises.key))

        when {
          controller.businessMatchingService.getAdditionalBusinessActivities(any(), any(), any())
        } thenReturn OptionT.none[Future, Set[BusinessActivity]]

        val result = controller.get()(request)
        status(result) must be(INTERNAL_SERVER_ERROR)

      }
    }
  }

  "post" must {

    "on valid request" must {
      "redirect to TradingPremises" when {
        "trading premises are selected and there are more activities through which to iterate" in new Fixture {

          mockApplicationStatus(SubmissionDecisionApproved)
          mockCacheFetch[Seq[TradingPremises]](Some(tradingPremises), Some(TradingPremises.key))

          when {
            controller.businessMatchingService.getAdditionalBusinessActivities(any(), any(), any())
          } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(HighValueDealing, MoneyServiceBusiness))

          when {
            controller.businessMatchingService.activitiesToIterate(any(),any())
          } thenReturn true

          val result = controller.post()(request.withFormUrlEncodedBody(
            "tradingPremises[]" -> "1"
          ))

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.businessmatching.updateservice.add.routes.TradingPremisesController.get().url))

        }
      }

      "progress to the 'New Service Information' page" when {
        "fit and proper is not required" in new Fixture {

          mockApplicationStatus(SubmissionDecisionApproved)
          mockCacheFetch[Seq[TradingPremises]](Some(tradingPremises), Some(TradingPremises.key))


          when {
            controller.businessMatchingService.getAdditionalBusinessActivities(any(),any(),any())
          } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(HighValueDealing))

          when {
            controller.businessMatchingService.fitAndProperRequired(any(),any(),any())
          } thenReturn OptionT.some[Future, Boolean](false)

          val result = controller.post(0)(request.withFormUrlEncodedBody("tradingPremises[]" -> "0"))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(add.routes.NewServiceInformationController.get().url)

        }
      }

      "progress to the 'fit and proper' page" when {
        "fit and proper requirement is introduced" in new Fixture {

          mockApplicationStatus(SubmissionDecisionApproved)
          mockCacheFetch[Seq[TradingPremises]](Some(tradingPremises), Some(TradingPremises.key))


          when {
            controller.businessMatchingService.getAdditionalBusinessActivities(any(),any(),any())
          } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(MoneyServiceBusiness))

          when {
            controller.businessMatchingService.fitAndProperRequired(any(),any(),any())
          } thenReturn OptionT.some[Future, Boolean](true)

          val result = controller.post(0)(request.withFormUrlEncodedBody("tradingPremises[]" -> "0"))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(add.routes.FitAndProperController.get().url)

        }
      }




//      "redirect to CurrentTradingPremises" when {
//        "trading premises are selected and there is an activity through which to iterate" in new Fixture {
//
//          mockApplicationStatus(SubmissionDecisionApproved)
//          mockCacheFetch[Seq[TradingPremises]](Some(tradingPremises), Some(TradingPremises.key))
//
//          when {
//            controller.businessMatchingService.getAdditionalBusinessActivities(any(), any(), any())
//          } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(HighValueDealing))
//
//          val result = controller.post()(request.withFormUrlEncodedBody(
//            "tradingPremises[]" -> "1"
//          ))
//
//          status(result) must be(SEE_OTHER)
//          redirectLocation(result) must be(Some(routes.CurrentTradingPremisesController.get(0).url))
//
//        }
//      }
    }

    "on invalid request" must {

      "return BAD_REQUEST" in new Fixture {

        mockApplicationStatus(SubmissionDecisionApproved)
        mockCacheFetch[Seq[TradingPremises]](Some(tradingPremises), Some(TradingPremises.key))

        when {
          controller.businessMatchingService.getAdditionalBusinessActivities(any(), any(), any())
        } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(HighValueDealing))

        val result = controller.post()(request)

        status(result) must be(BAD_REQUEST)

      }

    }

    "return NOT_FOUND" when {
      "status is pre-submission" in new Fixture {

        mockApplicationStatus(NotCompleted)
        mockCacheFetch[Seq[TradingPremises]](Some(tradingPremises), Some(TradingPremises.key))

        when {
          controller.businessMatchingService.getAdditionalBusinessActivities(any(), any(), any())
        } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set.empty)

        val result = controller.post()(request.withFormUrlEncodedBody(
          "tradingPremises[]" -> "1"
        ))

        status(result) must be(NOT_FOUND)

      }
      "there are no additional business activities" in new Fixture {

        mockApplicationStatus(SubmissionDecisionApproved)
        mockCacheFetch[Seq[TradingPremises]](Some(tradingPremises), Some(TradingPremises.key))

        when {
          controller.businessMatchingService.getAdditionalBusinessActivities(any(), any(), any())
        } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set.empty)

        val result = controller.post(3)(request.withFormUrlEncodedBody(
          "tradingPremises[]" -> "1"
        ))

        status(result) must be(NOT_FOUND)

      }
    }

    "return INTERNAL_SERVER_ERROR" when {

      "activities cannot be retrieved" in new Fixture {
        mockApplicationStatus(SubmissionDecisionApproved)

        when {
          controller.businessMatchingService.getAdditionalBusinessActivities(any(), any(), any())
        } thenReturn OptionT.none[Future, Set[BusinessActivity]]

        val result = controller.post()(request.withFormUrlEncodedBody(
          "tradingPremises[]" -> "1"
        ))

        status(result) must be(INTERNAL_SERVER_ERROR)

      }

    }

  }

}