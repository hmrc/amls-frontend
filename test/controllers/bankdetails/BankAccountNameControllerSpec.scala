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
import models.status.{SubmissionDecisionApproved, SubmissionReady}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.mockito.MockitoSugar
import play.api.test.Helpers._
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

class BankAccountNameControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends DependencyMocks { self =>

    val request = addToken(authRequest)

    val controller = new BankAccountNameController(
      SuccessfulAuthAction, ds = commonDependencies,
      mockCacheConnector,
      mockStatusService,
      mockMcc
    )
  }

  val fieldElements = Array("accountName")

  "BankAccountController" when {
    "get is called" must {
      "respond with OK" when {
        "without a name" in new Fixture {

          mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(None, None))), Some(BankDetails.key))

          mockApplicationStatus(SubmissionReady)

          val result = controller.getIndex(1, false)(request)
          val document: Document = Jsoup.parse(contentAsString(result))

          status(result) must be(OK)
          for (field <- fieldElements)
            document.select(s"input[name=$field]").`val` must be(empty)
        }

        "with a name" in new Fixture {

          val ukBankAccount = BankAccount(Some(BankAccountIsUk(true)), None, Some(UKAccount("12345678", "000000")))

          mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(None, Some("my bank account"), Some(ukBankAccount)))), Some(BankDetails.key))

          mockApplicationStatus(SubmissionReady)

          val result = controller.getIndex(1, true)(request)
          val document: Document = Jsoup.parse(contentAsString(result))
          status(result) must be(OK)
          for (field <- fieldElements)
            document.select(s"input[name=$field]").`val` must include("my bank account")
        }

        "there is no bank account information at all" in new Fixture {

          mockCacheFetch[Seq[BankDetails]](None, Some(BankDetails.key))

          mockApplicationStatus(SubmissionReady)

          val result = controller.getNoIndex(request)
          val document: Document = Jsoup.parse(contentAsString(result))
          status(result) must be(OK)
          for (field <- fieldElements)
            document.select(s"input[name=$field]").`val` must be(empty)
        }

        "editing a bank account" which {
          "hasn't been accepted or completed yet" in new Fixture {
            mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(None, None))), Some(BankDetails.key))

            mockApplicationStatus(SubmissionDecisionApproved)

            val result = controller.getIndex(1, edit = true)(request)

            status(result) must be(OK)
          }
        }
      }

      "respond with NOT_FOUND" when {
        "editing a bank account that has been accepted and completed" in new Fixture {
          mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(None, None, hasAccepted = true))), Some(BankDetails.key))

          mockApplicationStatus(SubmissionDecisionApproved)

          val result = controller.getIndex(1, false)(request)
          val document: Document = Jsoup.parse(contentAsString(result))

          status(result) mustBe NOT_FOUND
        }
      }

    }

    "post is called" must {
      "respond with SEE_OTHER" when {
        "given valid data in edit mode" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "accountName" -> "test"
           )

          mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(Some(PersonalAccount), None))), Some(BankDetails.key))

          mockCacheSave[Seq[BankDetails]]

          val result = controller.postIndex(1, true)(newRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.get(1).url))

        }
        "given valid data when NOT in edit mode" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "accountName" -> "test"
          )

          mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(Some(PersonalAccount), None))), Some(BankDetails.key))

          mockCacheSave[Seq[BankDetails]]

          val result = controller.postIndex(1)(newRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.BankAccountTypeController.get(1).url))
        }

      }

      "respond with NOT_FOUND" when {
        "given an index out of bounds in edit mode" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "accountName" -> "test"
          )

          mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(None, None))), Some(BankDetails.key))

          mockCacheSave[Seq[BankDetails]]

          val result = controller.postIndex(50, true)(newRequest)

          status(result) must be(NOT_FOUND)
        }
      }

      "respond with BAD_REQUEST" when {
        "given invalid data" in new Fixture {

          val newRequest = requestWithUrlEncodedBody(
            "accountName" -> ""
          )

          mockCacheFetch[Seq[BankDetails]](None, Some(BankDetails.key))
          mockCacheSave[Seq[BankDetails]]

          val result = controller.postIndex(1, true)(newRequest)

          status(result) must be(BAD_REQUEST)
        }
      }
    }
  }
}
