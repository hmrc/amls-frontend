package utils

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec

class StringHelperTest extends UnitSpec with MockitoSugar  {
  "isAllDigits" must {
    "return false if not all digits" in {
      StringHelper.isAllDigits("jjskl5jsdswj") shouldBe false
    }

    "return true if all digits" in {
      StringHelper.isAllDigits("384898347854") shouldBe true
    }
  }
}