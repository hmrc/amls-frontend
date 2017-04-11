package models.notifications

import org.joda.time.LocalDate
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec


class NotificationDetailsSpec extends PlaySpec with MustMatchers {

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

  "convertEndDateMessageText" must {
    "convert the input message text into the model when there is both a date and a red in the input string" in {

      val inputString = "parameter1-31/07/2018|parameter2-ABC1234"

      NotificationDetails.convertEndDateWithRefMessageText(inputString) mustBe Some(EndDateDetails(new LocalDate(2018,7,31), Some("ABC1234")))

    }

    "convert the input message text into the model when there is only a date in the input string" in {

      val inputString = "parameter1-31/07/2018"

      NotificationDetails.convertEndDateWithRefMessageText(inputString) mustBe Some(EndDateDetails(new LocalDate(2018,7,31), None))

    }

    "return none when supplied with an invalid string" in {
      val inputString = "invalidtest"

      NotificationDetails.convertEndDateWithRefMessageText(inputString) must be(None)
    }
  }
}
