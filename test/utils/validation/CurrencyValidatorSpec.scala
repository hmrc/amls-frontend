package utils.validation

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import uk.gov.hmrc.play.test.WithFakeApplication
import utils.validation.CurrencyValidator._
import play.api.data.FormError

class CurrencyValidatorSpec extends PlaySpec with MockitoSugar  with OneServerPerSuite {

  "currency" should {
    "return valid integer based values" in {
      optionalCurrency("error.currency").bind(Map("" -> "0")) mustBe Right(Some(0))
      optionalCurrency("error.currency").bind(Map("" -> "   0   ")) mustBe Right(Some(0))
      optionalCurrency("error.currency").bind(Map("" -> "1234")) mustBe Right(Some(1234))
      optionalCurrency("error.currency").bind(Map("" -> "1,234")) mustBe Right(Some(1234))
      optionalCurrency("error.currency").bind(Map("" -> "1,23,4")) mustBe Right(Some(1234))
      optionalCurrency("error.currency").bind(Map("" -> "99999999999")) mustBe Right(Some(BigDecimal("99999999999")))
    }

    "report an invalid money error" in {
      optionalCurrency("error.currency").bind(Map("" -> "£")) mustBe Left(List(FormError("", "error.currency")))
      optionalCurrency("error.currency").bind(Map("" -> "a")) mustBe Left(List(FormError("", "error.currency")))
      optionalCurrency("error.currency").bind(Map("" -> "1234.001")) mustBe Left(List(FormError("", "error.currency")))
      optionalCurrency("error.currency").bind(Map("" -> "1.234.001")) mustBe Left(List(FormError("", "error.currency")))
      optionalCurrency("error.currency").bind(Map("" -> "99999999999999999")) mustBe Left(List(FormError("", "error.currency")))
      optionalCurrency("error.currency").bind(Map("" -> "0.00")) mustBe Left(List(FormError("", "error.currency")))
      optionalCurrency("error.currency").bind(Map("" -> "1234.01")) mustBe Left(List(FormError("", "error.currency")))
      optionalCurrency("error.currency").bind(Map("" -> "1234.1")) mustBe Left(List(FormError("", "error.currency")))
      optionalCurrency("error.currency").bind(Map("" -> "-1234")) mustBe Left(List(FormError("", "error.currency")))
    }

    "report correctly for blank value" in {
      optionalCurrency("error.currency").bind(Map("" -> "")) mustBe Right(None)
    }
  }

}