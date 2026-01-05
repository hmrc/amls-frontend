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

package models.notifications

import models.confirmation.Currency
import models.notifications.RejectedReason.FailedToPayCharges
import models.notifications.StatusType.DeRegistered
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

import java.time.ZoneOffset._
import java.time.{Instant, LocalDate, LocalDateTime}

class NotificationDetailsSpec extends PlaySpec with Matchers {

  val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(1479730062573L), UTC)

  "NotificationDetails" must {

    "serialise json to model" in {

      val model = NotificationDetails(
        Some(ContactType.MindedToRevoke),
        Some(
          Status(
            Some(StatusType.Revoked),
            Some(RevokedReason.RevokedCeasedTrading)
          )
        ),
        Some("MessageText"),
        variation = false,
        dateTime
      )

      val json = Json.obj(
        "contactType" -> "MTRV",
        "status"      -> Json.obj(
          "status_type"   -> "08",
          "status_reason" -> "02"
        ),
        "messageText" -> "MessageText",
        "variation"   -> false,
        "receivedAt"  -> Json.obj("$date" -> Json.obj("$numberLong" -> "1479730062573"))
      )

      json.as[NotificationDetails] mustBe model
    }

  }

  "NotificationDetails.subject" must {

    "return the correct contactType when contactType is present" in {
      val details = NotificationDetails(Some(ContactType.AutoExpiryOfRegistration), None, None, false, dateTime)
      details.subject("v4m0") mustBe "notifications.subject.AutoExpiryOfRegistration"
    }

    "return the correct subject string" when {

      "the contact type is DeRegistrationEffectiveDateChange" in {
        val details = NotificationDetails(None, Some(Status(Some(DeRegistered), None)), None, false, dateTime)
        details.subject("v4m0") mustBe "notifications.subject.DeRegistrationEffectiveDateChange"
      }

      "the contact type is ApplicationAutorejectionForFailureToPay" in {
        val details = NotificationDetails(None, Some(Status(None, Some(FailedToPayCharges))), None, false, dateTime)
        details.subject("v4m0") mustBe "notifications.subject.ApplicationAutorejectionForFailureToPay"
      }

      "the contact type is RegistrationVariationApproval" in {
        val details = NotificationDetails(None, None, None, true, dateTime)
        details.subject("v4m0") mustBe "notifications.subject.RegistrationVariationApproval"
      }

    }

    "return return the no subject when the contact type cannot be figured out" in {
      val details = NotificationDetails(None, None, None, false, dateTime)
      details.subject("v4m0") mustBe "notifications.subject.NoSubject"
    }

  }

  "convertMessageText" must {
    "convert the input message text into the model" in {

      val inputString = "parameter1-1234|parameter2-ABC1234|Status-04-Approved"
      val receivedAt  = LocalDateTime.of(2025, 11, 25, 10, 0)

      NotificationDetails.convertReminderMessageText(inputString, receivedAt) mustBe Some(
        ReminderDetails(Currency(1234), "ABC1234", "23 December 2025")
      )

    }

    "return none when supplied with an invalid string" in {
      val inputString = "invalidtest"
      val receivedAt  = LocalDateTime.of(2025, 11, 25, 10, 0)

      NotificationDetails.convertReminderMessageText(inputString, receivedAt) must be(None)
    }
  }

  "convertEndDateWithRefMessageText" must {
    "convert the input message text into the model when there is both a date and a red in the input string" in {

      val inputString = "parameter 1-31/07/2018|parameter 2- ABC1234"

      // noinspection ScalaStyle
      NotificationDetails.convertEndDateWithRefMessageText(inputString) mustBe Some(
        EndDateDetails(LocalDate.of(2018, 7, 31), Some("ABC1234"))
      )

    }

    "convert the input message text into the model when there are special characters in the input string" in {
      val inputString = "<![CDATA[<P>parameter 1- 31/07/2018|parameter 2-ABC1234</P>]]>"

      // noinspection ScalaStyle
      NotificationDetails.convertEndDateWithRefMessageText(inputString) mustBe Some(
        EndDateDetails(LocalDate.of(2018, 7, 31), Some("ABC1234"))
      )

    }

    "return none when supplied with an invalid string" in {
      val inputString = "invalidtest"

      NotificationDetails.convertEndDateWithRefMessageText(inputString) must be(None)
    }
  }

  "convertEndDateMessageText" must {
    "convert the input message text into the model when there is only a date in the input string" in {

      val inputString = "parameter 1-31/07/2018"

      // noinspection ScalaStyle
      NotificationDetails.convertEndDateMessageText(inputString) mustBe Some(
        EndDateDetails(LocalDate.of(2018, 7, 31), None)
      )
    }

    "convert the input message text into the model when there are special characters in the input string" in {
      val inputString = "<![CDATA[<P>parameter 1-20/05/2009</P>]]>"

      // noinspection ScalaStyle
      NotificationDetails.convertEndDateMessageText(inputString) mustBe Some(
        EndDateDetails(LocalDate.of(2009, 5, 20), None)
      )
    }

    "return none when supplied with an invalid string" in {
      val inputString = "invalidtest"

      NotificationDetails.convertEndDateMessageText(inputString) must be(None)
    }
  }

  "processGenericMessage" must {

    "return the original text if not enclosed in CDATA tags" in {
      val inputString = "This is a message"

      NotificationDetails.processGenericMessage(inputString) mustBe inputString
    }

    "extract the text when it is defined within CDATA tags and" when {

      "the text is alphanumeric " in {
        val inputString = "<![CDATA[<P>Th1s 1s th3 m3ssag3</P>]]>"
        NotificationDetails.processGenericMessage(inputString) mustBe "<P>Th1s 1s th3 m3ssag3</P>"
      }

      "the text contains parentheses" in {
        val inputString = "<![CDATA[<P>Today is (Monday) 5th December</P>]]>"
        NotificationDetails.processGenericMessage(inputString) mustBe "<P>Today is (Monday) 5th December</P>"
      }

      "the text contains square brackets" in {
        val inputString = "<![CDATA[<P>Today is [Monday] 5th December</P>]]>"
        NotificationDetails.processGenericMessage(inputString) mustBe "<P>Today is [Monday] 5th December</P>"
      }

      "the text contains curly brackets" in {
        val inputString = "<![CDATA[<P>Today is {Monday} 5th December</P>]]>"
        NotificationDetails.processGenericMessage(inputString) mustBe "<P>Today is {Monday} 5th December</P>"
      }

      "the text contains plus symbols" in {
        val inputString = "<![CDATA[<P>Today is +Monday+ 5th December</P>]]>"
        NotificationDetails.processGenericMessage(inputString) mustBe "<P>Today is +Monday+ 5th December</P>"
      }

      "the text contains asterisks" in {
        val inputString = "<![CDATA[<P>Today is *Monday* 5th December</P>]]>"
        NotificationDetails.processGenericMessage(inputString) mustBe "<P>Today is *Monday* 5th December</P>"
      }

      "the text contains pipes" in {
        val inputString = "<![CDATA[<P>Today is |Monday| 5th December</P>]]>"
        NotificationDetails.processGenericMessage(inputString) mustBe "<P>Today is |Monday| 5th December</P>"
      }

      "the text contains dollar symbols" in {
        val inputString = "<![CDATA[<P>Today is $Monday$ 5th December</P>]]>"
        NotificationDetails.processGenericMessage(inputString) mustBe "<P>Today is $Monday$ 5th December</P>"
      }

      "the text contains carets" in {
        val inputString = "<![CDATA[<P>Today is ^Monday^ 5th December</P>]]>"
        NotificationDetails.processGenericMessage(inputString) mustBe "<P>Today is ^Monday^ 5th December</P>"
      }
    }
  }

}
