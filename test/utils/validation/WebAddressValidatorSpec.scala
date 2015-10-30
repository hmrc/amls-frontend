package utils.validation

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.FormError
import uk.gov.hmrc.play.test.WithFakeApplication
import utils.validation.WebAddressValidator._

class WebAddressValidatorSpec extends PlaySpec with MockitoSugar  with WithFakeApplication{

  "webAddress" should {
    "return the webAddress if the webAddress is correct" in {
      webAddress("invalid length", "invalid value")
        .bind(Map("" -> "http://www.foufos.gr")) mustBe Right("http://www.foufos.gr")
    }

    "return correct form error if the webAddress format is incorrect" in {
      webAddress("invalid length", "invalid value").bind(Map("" -> "a" * 101))
        .left.getOrElse(Nil).contains(FormError("", "invalid length")) mustBe true

      webAddress("invalid length", "invalid value").bind(Map("" -> "www.foufos"))
        .left.getOrElse(Nil).contains(FormError("", "invalid value")) mustBe true
    }
  }

}