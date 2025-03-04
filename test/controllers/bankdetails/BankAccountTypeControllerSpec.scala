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
import forms.bankdetails.BankAccountTypeFormProvider
import models.bankdetails.BankAccountType.{BelongsToBusiness, BelongsToOtherBusiness, NoBankAccountUsed, PersonalAccount}
import models.bankdetails._
import models.status.{SubmissionDecisionApproved, SubmissionReady, SubmissionReadyForReview}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Request, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import utils.{AmlsSpec, DependencyMocks, StatusConstants}
import views.html.bankdetails.BankAccountTypesView

import scala.concurrent.Future

class BankAccountTypeControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture extends DependencyMocks { self =>

    val request: Request[AnyContentAsEmpty.type] = addToken(authRequest)

    val accountType: BankAccountType.PersonalAccount.type = PersonalAccount

    val ukBankAccount: BankAccount =
      BankAccount(Some(BankAccountIsUk(true)), None, Some(UKAccount("123456", "11-11-11")))

    lazy val bankAccountTypes: BankAccountTypesView = inject[BankAccountTypesView]

    val controller = new BankAccountTypeController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      mockCacheConnector,
      mockStatusService,
      mockMcc,
      inject[BankAccountTypeFormProvider],
      bankAccountTypes,
      errorView
    )
  }

  "BankAccountTypeController" when {
    "get:" must {
      "respond with OK and display the blank 'bank account type' page" when {
        "there is any number of bank accounts" in new Fixture {

          mockCacheFetch[Seq[BankDetails]](
            Some(
              Seq(
                BankDetails(None, None, status = Some(StatusConstants.Deleted)),
                BankDetails(Some(NoBankAccountUsed), None, status = Some(StatusConstants.Added))
              )
            )
          )
          mockApplicationStatus(SubmissionReady)

          val result: Future[Result] = controller.get(1, false)(request)

          status(result)          must be(OK)
          contentAsString(result) must include(Messages("bankdetails.accounttype.title"))
          val document: Document = Jsoup.parse(contentAsString(result))

          document
            .select(s"input[type=radio][name=bankAccountType][value=${BelongsToOtherBusiness.toString}]")
            .hasAttr("checked")                                             must be(false)
          document
            .select(s"input[type=radio][name=bankAccountType][value=${BelongsToBusiness.toString}]")
            .hasAttr("checked")                                             must be(false)
          document
            .select(s"input[type=radio][name=bankAccountType][value=${PersonalAccount.toString}]")
            .hasAttr("checked")                                             must be(false)
          document.select("input[type=radio][name=bankAccountType]").size() must be(3)
        }

        "there is already a bank account type" in new Fixture {

          mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(Some(PersonalAccount), None))))
          mockApplicationStatus(SubmissionReady)

          val result: Future[Result] = controller.get(1)(request)
          val document: Boolean      = Jsoup
            .parse(contentAsString(result))
            .select(s"input[value=${PersonalAccount.toString}]")
            .hasAttr("checked")

          status(result) must be(OK)
          document       must be(true)
        }

        "editing a bank account" which {
          "hasn't been accepted or completed yet" in new Fixture {
            mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(Some(PersonalAccount), None))))
            mockApplicationStatus(SubmissionDecisionApproved)

            val result: Future[Result] = controller.get(1, edit = true)(request)
            val document: Boolean      = Jsoup
              .parse(contentAsString(result))
              .select(s"input[value=${PersonalAccount.toString}]")
              .hasAttr("checked")

            status(result) must be(OK)
            document       must be(true)
          }
        }
      }

      "respond with NOT_FOUND" when {
        "there is no bank account information at all" in new Fixture {

          mockCacheFetch[Seq[BankDetails]](None)
          mockApplicationStatus(SubmissionReady)

          val result: Future[Result] = controller.get(1, false)(request)

          status(result) must be(NOT_FOUND)
        }
        "editing an amendment" in new Fixture {

          mockCacheFetch[Seq[BankDetails]](
            Some(
              Seq(
                BankDetails(Some(accountType), Some("bankName"), Some(ukBankAccount), hasAccepted = true),
                BankDetails(Some(accountType), Some("bankName"), Some(ukBankAccount), hasAccepted = true)
              )
            ),
            Some(BankDetails.key)
          )

          mockApplicationStatus(SubmissionReadyForReview)

          val result: Future[Result] = controller.get(1, edit = true)(request)

          status(result) must be(NOT_FOUND)
        }

        "editing an variation" in new Fixture {

          mockCacheFetch[Seq[BankDetails]](
            Some(
              Seq(
                BankDetails(Some(accountType), Some("bankName"), Some(ukBankAccount), hasAccepted = true),
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
      "respond with OK and redirect to the bank account details page" when {

        "not editing and there is valid account type" in new Fixture {
          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.BankAccountTypeController.post(1, false).url)
              .withFormUrlEncodedBody(
                "bankAccountType" -> PersonalAccount.toString
              )

          mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(Some(PersonalAccount), None))))
          mockCacheSave[Seq[BankDetails]]

          val result: Future[Result] = controller.post(1, false)(newRequest)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.BankAccountIsUKController.get(1, false).url))
        }

        "not editing and there is no bank account" in new Fixture {

          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.BankAccountTypeController.post(1, false).url)
              .withFormUrlEncodedBody(
                "bankAccountType" -> NoBankAccountUsed.toString
              )

          mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(None, None))))
          mockCacheSave[Seq[BankDetails]]

          val result: Future[Result] = controller.post(1, false)(newRequest)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.get(1).url))
        }

        "editing and there is valid account type but no account details" in new Fixture {
          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.BankAccountTypeController.post(1, edit = true).url)
              .withFormUrlEncodedBody(
                "bankAccountType" -> PersonalAccount.toString
              )

          mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(Some(PersonalAccount), None))))
          mockCacheSave[Seq[BankDetails]]

          val result: Future[Result] = controller.post(1, edit = true)(newRequest)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.get(1).url))
        }

        "editing and there is both a valid account type and valid account details" in new Fixture {
          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.BankAccountTypeController.post(1, edit = true).url)
              .withFormUrlEncodedBody(
                "bankAccountType" -> PersonalAccount.toString
              )

          mockCacheFetch[Seq[BankDetails]](
            Some(
              Seq(
                BankDetails(
                  Some(PersonalAccount),
                  Some("AccountName"),
                  Some(ukBankAccount)
                )
              )
            )
          )

          mockCacheSave[Seq[BankDetails]]

          val result: Future[Result] = controller.post(1, edit = true)(newRequest)

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.get(1).url))
        }
      }

      "respond with BAD_REQUEST" when {
        "there is invalid data" in new Fixture {
          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.BankAccountTypeController.post(0, false).url)
              .withFormUrlEncodedBody(
                "bankAccountType" -> "foo"
              )

          mockCacheFetch[Seq[BankDetails]](None)
          mockCacheSave[Seq[BankDetails]]

          val result: Future[Result] = controller.post(0, false)(newRequest)

          status(result) must be(BAD_REQUEST)
        }
      }

      "respond with NOT_FOUND" when {
        "the given index is out of bounds" in new Fixture {
          val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
            FakeRequest(POST, routes.BankAccountTypeController.post(1, false).url)
              .withFormUrlEncodedBody(
                "bankAccountType" -> PersonalAccount.toString
              )

          mockApplicationStatus(SubmissionDecisionApproved)

          mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(None, None))), Some(BankDetails.key))

          when(mockCacheConnector.save[Seq[BankDetails]](any(), any(), any())(any()))
            .thenThrow(new IndexOutOfBoundsException("error"))

          val result: Future[Result] = controller.post(1, false)(newRequest)

          status(result) must be(NOT_FOUND)
        }
      }
    }
  }
}
