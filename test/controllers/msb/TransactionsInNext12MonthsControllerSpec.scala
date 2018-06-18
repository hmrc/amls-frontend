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

package controllers.msb

import models.businessmatching.{MoneyServiceBusiness => MoneyServiceBusinessActivity}
import models.moneyservicebusiness.{MoneyServiceBusiness, SendMoneyToOtherCountry, TransactionsInNext12Months}
import models.status.{NotCompleted, SubmissionDecisionApproved, SubmissionDecisionRejected}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

import scala.concurrent.Future

class TransactionsInNext12MonthsControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>
    val request = addToken(authRequest)

    val controller = new TransactionsInNext12MonthsController(self.authConnector,
      mockCacheConnector,
      mockStatusService,
      mockServiceFlow
    )

    mockIsNewActivity(false)
  }

  val emptyCache = CacheMap("", Map.empty)

  "TransactionsInNext12MonthsController" must {

    "load the page 'How many transactions do you expect in the next 12 months?'" in new Fixture {

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(NotCompleted))

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("msb.transactions.expected.title"))
    }

    "load the page 'How many transactions do you expect in the next 12 months?' with pre populated data" in new Fixture {

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(NotCompleted))

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(MoneyServiceBusiness(
        transactionsInNext12Months = Some(TransactionsInNext12Months("12345678963"))))))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include("12345678963")

    }

    "load the page 'How many transactions do you expect in the next 12 months?'" when {
      "status is approved and the service has just been added" in new Fixture {
        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        mockIsNewActivity(true, Some(MoneyServiceBusinessActivity))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("msb.transactions.expected.title"))
      }
    }

    "redirect to Page not found" when {
      "application is in variation mode" in new Fixture {

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        val result = controller.get()(request)
        status(result) must be(NOT_FOUND)
      }
    }

    "redirect to Page not found" when {
      "application is in variation mode and status is SubmissionDecisionRejected" in new Fixture {

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionRejected))

        val result = controller.get()(request)
        status(result) must be(NOT_FOUND)
      }
    }


    "Show error message when user has not filled the mandatory fields" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "txnAmount" -> ""
      )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("error.required.msb.transactions.in.12months"))

    }

    "on valid post" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "txnAmount" -> "12345678963"
      )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.SendMoneyToOtherCountryController.get().url))
    }

    "on valid post in edit mode with the next page's data in the store" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "txnAmount" -> "12345678963"
      )

      val incomingModel = MoneyServiceBusiness(
        sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true))
      )

      val outgoingModel = incomingModel.copy(
        transactionsInNext12Months = Some(
          TransactionsInNext12Months("12345678963")
        ), hasChanged = true
      )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))
        (any(), any(), any())).thenReturn(Future.successful(Some(incomingModel)))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.SummaryController.get().url))
    }

    "on valid post in edit mode without the next page's data in the store" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "txnAmount" -> "12345678963"
      )

      val incomingModel = MoneyServiceBusiness()

      val outgoingModel = incomingModel.copy(
        transactionsInNext12Months = Some(
          TransactionsInNext12Months("12345678963")
        ), hasChanged = true
      )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))
        (any(), any(), any())).thenReturn(Future.successful(Some(incomingModel)))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), eqTo(outgoingModel))
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.msb.routes.SendMoneyToOtherCountryController.get(true).url))
    }
  }
}
