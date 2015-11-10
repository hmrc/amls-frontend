package utils.validation

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.data.FormError
import utils.validation.CurrencyValidator._

class CurrencyValidatorSpec extends PlaySpec with MockitoSugar  with OneServerPerSuite {

  "currency" should {
    "return valid currency values" in {
      optionalCurrency("invalid").bind(Map("" -> "GBP")) mustBe Right(Some("GBP"))
      optionalCurrency("invalid").bind(Map("" -> "GBP ")) mustBe Right(Some("GBP"))
      optionalCurrency("invalid").bind(Map("" -> "EGP")) mustBe Right(Some("EGP"))
      optionalCurrency("invalid").bind(Map("" -> "USD")) mustBe Right(Some("USD"))
      optionalCurrency("invalid").bind(Map("" -> "SEK")) mustBe Right(Some("SEK"))

    }

    "report an invalid currency error" in {
      optionalCurrency("invalid").bind(Map("" -> "ZZZ")) mustBe Left(List(FormError("", "invalid")))
      optionalCurrency("invalid").bind(Map("" -> "!£$")) mustBe Left(List(FormError("", "invalid")))
    }

    "report correctly for blank value" in {
      optionalCurrency("invalid").bind(Map("" -> "")) mustBe Right(None)
    }

    "respond appropriately if unbound" in {
      optionalCurrency("invalid").binder.unbind("", Some("GBP")) mustBe Map("" -> "GBP")
    }

  }

}