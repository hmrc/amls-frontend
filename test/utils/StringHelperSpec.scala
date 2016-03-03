package utils

import org.scalatest.mock.MockitoSugar
import play.api.data.mapping.forms.Rules
import play.api.data.mapping.{Success, Rule}
import uk.gov.hmrc.play.test.UnitSpec

class StringHelperSpec extends UnitSpec with MockitoSugar  {
  "isAllDigits" must {
    "return false if not all digits" in {
      StringHelper.isAllDigits("jjskl5jsdswj") shouldBe false
    }

    "return true if all digits" in {
      StringHelper.isAllDigits("384898347854") shouldBe true
    }
  }

}
