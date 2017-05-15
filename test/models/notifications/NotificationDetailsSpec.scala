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

package models.notifications

import models.notifications.RejectedReason.FailedToPayCharges
import models.notifications.StatusType.DeRegistered
import org.joda.time.LocalDate
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec


class NotificationDetailsSpec extends PlaySpec with MustMatchers {


  "NotificationDetails.subject" must {

    "return the correct contactType when contactType is present" in {
      val details = NotificationDetails(Some(ContactType.AutoExpiryOfRegistration), None, None, false)
      details.subject mustBe "notifications.subject.AutoExpiryOfRegistration"
    }

    "return the correct subject string when the contact type is DeRegistrationEffectiveDateChange" in {
      val details = NotificationDetails(None, Some(Status(Some(DeRegistered), None)), None, false)
      details.subject mustBe "notifications.subject.DeRegistrationEffectiveDateChange"
    }
    "return the correct subject string when the contact type is ApplicationAutorejectionForFailureToPay" in {
      val details = NotificationDetails(None, Some(Status(None, Some(FailedToPayCharges))), None, false)
      details.subject mustBe "notifications.subject.ApplicationAutorejectionForFailureToPay"
    }
    "return the correct subject string when the contact type is RegistrationVariationApproval" in {
      val details = NotificationDetails(None, None, None, true)
      details.subject mustBe "notifications.subject.RegistrationVariationApproval"
    }
    "return a runtime Exception when the contact type cannot be figured out" in {
      val details = NotificationDetails(None, None, None, false)
      val thrown = intercept[Exception] {
        details.subject
      }

      assert(thrown.getMessage === "No matching ContactType found")
    }
  }

  "convertMessageText" must {
    "convert the input message text into the model" in {

      val inputString = "parameter1-1234|parameter2-ABC1234|Status-04-Approved"

      NotificationDetails.convertReminderMessageText(inputString) mustBe Some(ReminderDetails(1234, "ABC1234"))

    }

    "return none when supplied with an invalid string" in {
      val inputString = "invalidtest"

      NotificationDetails.convertReminderMessageText(inputString) must be(None)
    }
  }

  "convertEndDateWithRefMessageText" must {
    "convert the input message text into the model when there is both a date and a red in the input string" in {

      val inputString = "parameter1-31/07/2018|parameter2-ABC1234"

      NotificationDetails.convertEndDateWithRefMessageText(inputString) mustBe Some(EndDateDetails(new LocalDate(2018,7,31), Some("ABC1234")))

    }

    "return none when supplied with an invalid string" in {
      val inputString = "invalidtest"

      NotificationDetails.convertEndDateWithRefMessageText(inputString) must be(None)
    }
  }

  "convertEndDateMessageText" must {
    "convert the input message text into the model when there is only a date in the input string" in {

      val inputString = "parameter1-31/07/2018"

      NotificationDetails.convertEndDateMessageText(inputString) mustBe Some(EndDateDetails(new LocalDate(2018, 7, 31), None))
    }

    "return none when supplied with an invalid string" in {
      val inputString = "invalidtest"

      NotificationDetails.convertEndDateMessageText(inputString) must be(None)
    }
  }
}
