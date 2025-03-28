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

package controllers.bankdetails

import controllers.actions.SuccessfulAuthAction
import forms.bankdetails.BankAccountUKFormProvider
import models.bankdetails.BankAccountType.PersonalAccount
import models.bankdetails._
import models.status.{SubmissionDecisionApproved, SubmissionReady, SubmissionReadyForReview}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.ArgumentCaptor
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Request, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}
import views.html.bankdetails.BankAccountUKView

import scala.concurrent.Future

class BankAccountUKControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>

    val request: Request[AnyContentAsEmpty.type] = addToken(authRequest)

    val ukBankAccount: BankAccount =
      BankAccount(Some(BankAccountIsUk(true)), None, Some(UKAccount("12345678", "11-11-11")))

    val accountType: BankAccountType.PersonalAccount.type = PersonalAccount

    val bankAcc: BankAccountUKView = inject[BankAccountUKView]

    val controller = new BankAccountUKController(
      mockCacheConnector,
      SuccessfulAuthAction,
      mock[AuditConnector],
      mockStatusService,
      commonDependencies,
      mockMcc,
      inject[BankAccountUKFormProvider],
      bankAcc,
      errorView
    )

  }

  val fieldElements: Array[String] = Array("accountNumber", "sortCode")

  "BankAccountUKController" when {
    "get is called" must {
      "respond with OK" when {
        "there is no bank account detail information yet" in new Fixture {

          mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(None, None))), Some(BankDetails.key))

          mockApplicationStatus(SubmissionReady)

          val result: Future[Result] = controller.get(1, false)(request)
          val document: Document     = Jsoup.parse(contentAsString(result))

          status(result) must be(OK)
          for (field <- fieldElements)
            document.select(s"input[name=$field]").`val` must be(empty)
        }

        "there is already bank account detail information" in new Fixture {

          mockCacheFetch[Seq[BankDetails]](
            Some(Seq(BankDetails(None, None, Some(ukBankAccount)))),
            Some(BankDetails.key)
          )

          mockApplicationStatus(SubmissionReady)

          val result: Future[Result] = controller.get(1, edit = true)(request)
          status(result) must be(OK)
        }

        "when editing a bank account" which {
          "hasn't been accepted and completed yet" in new Fixture {
            mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(None, None))), Some(BankDetails.key))

            mockApplicationStatus(SubmissionDecisionApproved)

            val result: Future[Result] = controller.get(1, edit = true)(request)
            val document: Document     = Jsoup.parse(contentAsString(result))

            status(result) must be(OK)

            for (field <- fieldElements)
              document.select(s"input[name=$field]").`val` must be(empty)
          }
        }
      }

      "respond with NOT_FOUND" when {
        "there is no bank account information at all" in new Fixture {

          mockCacheFetch[Seq[BankDetails]](None, Some(BankDetails.key))

          mockApplicationStatus(SubmissionReady)

          val result: Future[Result] = controller.get(1)(request)

          status(result) must be(NOT_FOUND)
        }

        "editing an amendment" in new Fixture {

          mockCacheFetch[Seq[BankDetails]](
            Some(
              Seq(
                BankDetails(Some(accountType), Some("bankName"), Some(ukBankAccount), hasAccepted = true)
              )
            ),
            Some(BankDetails.key)
          )

          mockApplicationStatus(SubmissionReadyForReview)

          val result: Future[Result] = controller.get(1, edit = true)(request)

          status(result) must be(NOT_FOUND)

        }

        "editing a variation" in new Fixture {

          mockCacheFetch[Seq[BankDetails]](
            Some(
              Seq(
                BankDetails(Some(accountType), Some("bankName"), Some(ukBankAccount), hasAccepted = true)
              )
            ),
            Some(BankDetails.key)
          )

          mockApplicationStatus(SubmissionDecisionApproved)

          val result: Future[Result] = controller.get(1, edit = true)(request)

          status(result) must be(NOT_FOUND)

        }
      }
    }

    "post is called" must {
      "respond with SEE_OTHER" when {
        "given valid data in edit mode" in new Fixture {

          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.BankAccountUKController.post(1, edit = true).url)
              .withFormUrlEncodedBody(
                "accountNumber" -> "12345678",
                "sortCode"      -> "123456"
              )

          when(controller.auditConnector.sendEvent(any())(any(), any()))
            .thenReturn(Future.successful(AuditResult.Success))

          mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(Some(PersonalAccount), None))), Some(BankDetails.key))
          mockCacheSave[Seq[BankDetails]]

          val result: Future[Result] = controller.post(1, edit = true)(newRequest)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.get(1).url))
        }
        "given valid data when NOT in edit mode" in new Fixture {

          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.BankAccountUKController.post(1, false).url)
              .withFormUrlEncodedBody(
                "accountNumber" -> "12345678",
                "sortCode"      -> "123456"
              )

          when(controller.auditConnector.sendEvent(any())(any(), any()))
            .thenReturn(Future.successful(Success))

          mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(Some(PersonalAccount), None))), Some(BankDetails.key))
          mockCacheSave[Seq[BankDetails]]

          val result: Future[Result] = controller.post(1)(newRequest)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.get(1).url))
        }

      }

      "respond with NOT_FOUND" when {
        "given an index out of bounds in edit mode" in new Fixture {

          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.BankAccountUKController.post(50, edit = true).url)
              .withFormUrlEncodedBody(
                "accountNumber" -> "12345678",
                "sortCode"      -> "123456"
              )

          mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(None, None))), Some(BankDetails.key))
          mockCacheSave[Seq[BankDetails]]

          val result: Future[Result] = controller.post(50, edit = true)(newRequest)

          status(result) must be(NOT_FOUND)
        }
      }

      "respond with BAD_REQUEST" when {
        "given invalid data" in new Fixture {

          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.BankAccountUKController.post(1, edit = true).url)
              .withFormUrlEncodedBody(
                "accountNumber" -> "%!@£%",
                "sortCode"      -> "&^%$"
              )

          mockCacheFetch[Seq[BankDetails]](None, Some(BankDetails.key))
          mockCacheSave[Seq[BankDetails]]

          val result: Future[Result] = controller.post(1, edit = true)(newRequest)

          status(result) must be(BAD_REQUEST)
        }
      }
    }

    "an account is created" must {
      "send an audit event" in new Fixture {
        val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          FakeRequest(POST, routes.BankAccountUKController.post(1, false).url)
            .withFormUrlEncodedBody(
              "accountNumber" -> "12345678",
              "sortCode"      -> "123456"
            )

        when(controller.auditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(Success))

        mockCacheFetch[Seq[BankDetails]](
          Some(Seq(BankDetails(Some(PersonalAccount), Some("Test account"), Some(ukBankAccount)))),
          Some(BankDetails.key)
        )

        mockCacheSave[Seq[BankDetails]]

        val result: Future[Result] = controller.post(1)(newRequest)

        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get(1).url))

        val captor = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(controller.auditConnector).sendEvent(captor.capture())(any(), any())

        captor.getValue match {
          case DataEvent(_, _, _, _, detail, _, _, _, _) =>
            detail("accountName") mustBe "Test account"
        }

      }
    }
  }
}
