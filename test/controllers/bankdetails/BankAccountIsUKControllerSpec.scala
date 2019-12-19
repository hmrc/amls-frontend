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

package controllers.bankdetails

import controllers.actions.SuccessfulAuthAction
import models.bankdetails._
import models.status.{SubmissionDecisionApproved, SubmissionReady, SubmissionReadyForReview}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.{ArgumentCaptor, Matchers}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

import scala.concurrent.Future

class BankAccountIsUKControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends DependencyMocks { self =>

    val request = addToken(authRequest)


    val ukBankAccount = BankAccount(Some(BankAccountIsUk(true)), None, Some(UKAccount("123456", "11-11-11")))

    val controller = new BankAccountIsUKController(
      mockCacheConnector,
      SuccessfulAuthAction, ds = commonDependencies,
      mock[AuditConnector],
      mockStatusService,
      mockMcc
    )

  }


  "BankAccountIsUKController" when {
    "get is called" must {
      "respond with OK" when {

        "there is already bank account detail information" in new Fixture {

          mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(None, None, Some(ukBankAccount)))), Some(BankDetails.key))

          mockApplicationStatus(SubmissionReady)

          val result = controller.get(1, true)(request)
          status(result) must be(OK)
        }
      }

      "respond with NOT_FOUND" when {
        "there is no bank account information at all" in new Fixture {

          mockCacheFetch[Seq[BankDetails]](None, Some(BankDetails.key))

          mockApplicationStatus(SubmissionReady)

          val result = controller.get(1)(request)

          status(result) must be(NOT_FOUND)
        }

        "editing an amendment" in new Fixture {


          mockCacheFetch[Seq[BankDetails]](Some(Seq(
            BankDetails(None, None, Some(ukBankAccount), hasAccepted = true))), Some(BankDetails.key))

          mockApplicationStatus(SubmissionReadyForReview)

          val result = controller.get(1, true)(request)

          status(result) must be(NOT_FOUND)

        }

        "editing a variation" in new Fixture {

          mockCacheFetch[Seq[BankDetails]](Some(Seq(
            BankDetails(None, None, Some(ukBankAccount), hasAccepted = true))), Some(BankDetails.key))

          mockApplicationStatus(SubmissionDecisionApproved)

          val result = controller.get(1, true)(request)

          status(result) must be(NOT_FOUND)

        }
      }
    }

    "post is called" must {
      "respond with SEE_OTHER" when {
        "given valid data in edit mode" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "isUK" -> "false"
          )

          when(controller.auditConnector.sendEvent(Matchers.any())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(AuditResult.Success))

          mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(Some(PersonalAccount), None))), Some(BankDetails.key))
          mockCacheSave[Seq[BankDetails]]

          val result = controller.post(1, true)(newRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.BankAccountHasIbanController.get(1).url))
        }

        "given valid data when NOT in edit mode" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "isUK" -> "false"
          )

          when(controller.auditConnector.sendEvent(any())(any(), any()))
            .thenReturn(Future.successful(Success))

          mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(Some(PersonalAccount), None))), Some(BankDetails.key))
          mockCacheSave[Seq[BankDetails]]

          val result = controller.post(1)(newRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.BankAccountHasIbanController.get(1).url))
        }

      }

      "respond with NOT_FOUND" when {
        "given an index out of bounds in edit mode" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "isUK" -> "false"
          )

          mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(None, None))), Some(BankDetails.key))
          mockCacheSave[Seq[BankDetails]]

          val result = controller.post(50, true)(newRequest)

          status(result) must be(NOT_FOUND)
        }
      }

      "respond with BAD_REQUEST" when {
        "given invalid data" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "isUK" -> ""
          )

          mockCacheFetch[Seq[BankDetails]](None, Some(BankDetails.key))
          mockCacheSave[Seq[BankDetails]]

          val result = controller.post(1, true)(newRequest)

          status(result) must be(BAD_REQUEST)
        }
      }
    }

    "an account is created" must {
      "send an audit event" in new Fixture {
        val newRequest = requestWithUrlEncodedBody(
          "isUK" -> "false",
          "nonUKAccountNumber" -> "1234567890123456789012345678901234567890",
          "isIBAN" -> "false"
        )

        when(controller.auditConnector.sendEvent(any())(any(), any())).
          thenReturn(Future.successful(Success))

        mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(
          Some(PersonalAccount),
          Some("Test account"),
          Some(UKAccount("8934798324", "934947"))))),
          Some(BankDetails.key))

        mockCacheSave[Seq[BankDetails]]

        val result = controller.post(1)(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get(1).url))

        val captor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(controller.auditConnector).sendEvent(captor.capture())(any(), any())

        captor.getValue match {
          case DataEvent(_, _, _, _, detail, _) =>
            detail("accountName") mustBe "Test account"
        }

      }
    }
  }
}
