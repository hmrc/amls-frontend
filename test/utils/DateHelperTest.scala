package utils

import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec

class DateHelperTest extends UnitSpec with MockitoSugar  {
  "isNotFutureDate" must {
    "return false if the date is later than current date" in {
      DateHelper.isNotFutureDate(LocalDate.now().plusDays(1)) shouldBe false
    }

    "return true if the date is equal to or earlier than the current date" in {
      DateHelper.isNotFutureDate(LocalDate.now()) shouldBe true
      DateHelper.isNotFutureDate(LocalDate.now().minusDays(1)) shouldBe true
    }
  }
}