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

package utils

import models.aboutthebusiness.{RegisteredOffice, RegisteredOfficeUK}
import models.status.{ReadyForRenewal, RenewalSubmitted, SubmissionDecisionApproved, SubmissionReadyForReview}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.test.FakeApplication
import uk.gov.hmrc.play.test.UnitSpec

class DateOfChangeHelperSpec extends UnitSpec with OneAppPerSuite with MockitoSugar {

  "DateOfChangeHelper" should {

    object DateOfChangeHelperTest extends DateOfChangeHelper{
    }

    val originalModel = RegisteredOfficeUK(
      "addressLine1",
      "addressLine2",
      None,
      None,
      "postCode",
      None
    )

    val changeModel = RegisteredOfficeUK("","",None, None, "", None)

    "return true" when {
      "a change has been made to a model" in {
        DateOfChangeHelperTest.redirectToDateOfChange[RegisteredOffice](SubmissionDecisionApproved, Some(originalModel), changeModel) should be(true)
      }
    }

    "return false" when {
      "no change has been made to a model" in {
        DateOfChangeHelperTest.redirectToDateOfChange[RegisteredOffice](SubmissionDecisionApproved, Some(originalModel), originalModel) should be(false)
      }
    }

    "return isEligibleForDateOfChange false when status is Amendment" in {
      DateOfChangeHelperTest.redirectToDateOfChange[RegisteredOffice](SubmissionReadyForReview, Some(originalModel), changeModel) should be(false)
    }

    "return isEligibleForDateOfChange true when status is Variation" in {
      DateOfChangeHelperTest.redirectToDateOfChange[RegisteredOffice](SubmissionDecisionApproved, Some(originalModel), changeModel) should be(true)
    }

    "return isEligibleForDateOfChange true when status is Renewal" in {
      DateOfChangeHelperTest.redirectToDateOfChange[RegisteredOffice](ReadyForRenewal(None), Some(originalModel), changeModel) should be(true)
    }

    "return isEligibleForDateOfChange true when status is Renewal Submitted" in {
      DateOfChangeHelperTest.redirectToDateOfChange[RegisteredOffice](RenewalSubmitted(None), Some(originalModel), changeModel) should be(true)
    }

  }

}
