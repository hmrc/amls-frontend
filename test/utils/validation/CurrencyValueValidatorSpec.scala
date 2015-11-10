package utils.validation

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import utils.validation.CurrencyValueValidator._
import play.api.data.FormError

class CurrencyValueValidatorSpec extends PlaySpec with MockitoSugar  with OneServerPerSuite {

  "currency" should {
    "return valid integer based values" in {
      optionalCurrencyValue("error.currency").bind(Map("" -> "0")) mustBe Right(Some(0))
      optionalCurrencyValue("error.currency").bind(Map("" -> "   0   ")) mustBe Right(Some(0))
      optionalCurrencyValue("error.currency").bind(Map("" -> "1234")) mustBe Right(Some(1234))
      optionalCurrencyValue("error.currency").bind(Map("" -> "1,234")) mustBe Right(Some(1234))
      optionalCurrencyValue("error.currency").bind(Map("" -> "1,23,4")) mustBe Right(Some(1234))
      optionalCurrencyValue("error.currency").bind(Map("" -> "99999999999")) mustBe Right(Some(BigDecimal("99999999999")))
    }

    "report an invalid money error" in {
      optionalCurrencyValue("error.currency").bind(Map("" -> "£")) mustBe Left(List(FormError("", "error.currency")))
      optionalCurrencyValue("error.currency").bind(Map("" -> "a")) mustBe Left(List(FormError("", "error.currency")))
      optionalCurrencyValue("error.currency").bind(Map("" -> "1234.001")) mustBe Left(List(FormError("", "error.currency")))
      optionalCurrencyValue("error.currency").bind(Map("" -> "1.234.001")) mustBe Left(List(FormError("", "error.currency")))
      optionalCurrencyValue("error.currency").bind(Map("" -> "99999999999999999")) mustBe Left(List(FormError("", "error.currency")))
      optionalCurrencyValue("error.currency").bind(Map("" -> "0.00")) mustBe Left(List(FormError("", "error.currency")))
      optionalCurrencyValue("error.currency").bind(Map("" -> "1234.01")) mustBe Left(List(FormError("", "error.currency")))
      optionalCurrencyValue("error.currency").bind(Map("" -> "1234.1")) mustBe Left(List(FormError("", "error.currency")))
      optionalCurrencyValue("error.currency").bind(Map("" -> "-1234")) mustBe Left(List(FormError("", "error.currency")))
    }

    "report correctly for blank value" in {
      optionalCurrencyValue("error.currency").bind(Map("" -> "")) mustBe Right(None)
    }

    "respond appropriately if unbound" in {
      optionalCurrencyValue("error.currency").binder.unbind("", Some(BigDecimal(44) )) mustBe Map("" -> "44")
    }

  }

}