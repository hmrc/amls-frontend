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

package utils

import controllers.hvd.routes
import models.businessdetails.{RegisteredOffice, RegisteredOfficeUK}
import models.status.{ReadyForRenewal, RenewalSubmitted, SubmissionDecisionApproved, SubmissionReadyForReview}

class DateOfChangeHelperSpec extends AmlsSpec {

  "DateOfChangeHelper" should {

    object DateOfChangeHelperTest extends DateOfChangeHelper {}

    val originalModel = RegisteredOfficeUK(
      "addressLine1",
      None,
      None,
      None,
      "postCode",
      None
    )

    val changeModel = RegisteredOfficeUK("", None, None, None, "", None)
    "DateOfChangeHelper" must {

      "redirect to Summary Controller" when {
        "not supported key is passed" in {
          DateOfChangeHelperTest.DateOfChangeRedirect("blah").call.url mustBe routes.SummaryController.get.url
        }
      }

      "redirect to Summary Controller" when {
        "1 is passed" in {
          DateOfChangeHelperTest.DateOfChangeRedirect("1").call.url mustBe routes.SummaryController.get.url
        }
      }

      "redirect to CashPayment Controller" when {
        "2 is passed" in {
          DateOfChangeHelperTest.DateOfChangeRedirect("2").call.url mustBe routes.CashPaymentController.get().url
        }
      }

      "redirect to HowWillYouSellGoods Controller" when {
        "3 is passed" in {
          DateOfChangeHelperTest.DateOfChangeRedirect("3").call.url mustBe routes.HowWillYouSellGoodsController
            .get()
            .url
        }
      }

      "redirect to ExciseGoods Controller" when {
        "4 is passed" in {
          DateOfChangeHelperTest.DateOfChangeRedirect("4").call.url mustBe routes.ExciseGoodsController.get().url
        }

        "redirect to ExciseGoods Controller with edit set to true" when {
          "5 is passed" in {
            DateOfChangeHelperTest
              .DateOfChangeRedirect("5")
              .call
              .url mustBe s"${routes.ExciseGoodsController.get().url}?edit=true"
          }
        }

      }

    }

    "return empty list" when {
      "no start date is supplied" in {
        DateOfChangeHelperTest.startDateFormFields(None) must be(Map.empty[String, Seq[String]])
      }
    }

    "return true" when {
      "a change has been made to a model" in {
        DateOfChangeHelperTest.redirectToDateOfChange[RegisteredOffice](
          SubmissionDecisionApproved,
          Some(originalModel),
          changeModel
        ) must be(true)
      }
    }

    "return false" when {
      "no change has been made to a model" in {
        DateOfChangeHelperTest.redirectToDateOfChange[RegisteredOffice](
          SubmissionDecisionApproved,
          Some(originalModel),
          originalModel
        ) must be(false)
      }
    }

    "return isEligibleForDateOfChange false when status is Amendment" in {
      DateOfChangeHelperTest.redirectToDateOfChange[RegisteredOffice](
        SubmissionReadyForReview,
        Some(originalModel),
        changeModel
      ) must be(false)
    }

    "return isEligibleForDateOfChange true when status is Variation" in {
      DateOfChangeHelperTest.redirectToDateOfChange[RegisteredOffice](
        SubmissionDecisionApproved,
        Some(originalModel),
        changeModel
      ) must be(true)
    }

    "return isEligibleForDateOfChange true when status is Renewal" in {
      DateOfChangeHelperTest
        .redirectToDateOfChange[RegisteredOffice](ReadyForRenewal(None), Some(originalModel), changeModel) must be(true)
    }

    "return isEligibleForDateOfChange true when status is Renewal Submitted" in {
      DateOfChangeHelperTest.redirectToDateOfChange[RegisteredOffice](
        RenewalSubmitted(None),
        Some(originalModel),
        changeModel
      ) must be(true)
    }

    "dateOfChangApplicable return false when models not changed" in {
      DateOfChangeHelperTest.dateOfChangApplicable(
        "Approved",
        Some(originalModel),
        originalModel
      ) must be(false)
    }

    "dateOfChangApplicable return false when status not Approved" in {
      DateOfChangeHelperTest.dateOfChangApplicable(
        "NotYestSubmitted",
        Some(originalModel),
        changeModel
      ) must be(false)
    }

    "dateOfChangApplicable return true when models changed and status is Approved" in {
      DateOfChangeHelperTest.dateOfChangApplicable(
        "Approved",
        Some(originalModel),
        changeModel
      ) must be(true)
    }
  }
}
