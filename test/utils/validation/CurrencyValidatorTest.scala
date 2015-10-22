package utils.validation

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import utils.validation.CurrencyValidator._
import play.api.data.FormError

class CurrencyValidatorTest extends UnitSpec with MockitoSugar with amls.FakeAmlsApp {

  "currency" should {
    "return valid integer based values" in {
      optionalCurrency("error.currency").bind(Map("" -> "0")) shouldBe Right(Some(0))
      optionalCurrency("error.currency").bind(Map("" -> "   0   ")) shouldBe Right(Some(0))
      optionalCurrency("error.currency").bind(Map("" -> "1234")) shouldBe Right(Some(1234))
      optionalCurrency("error.currency").bind(Map("" -> "1,234")) shouldBe Right(Some(1234))
      optionalCurrency("error.currency").bind(Map("" -> "1,23,4")) shouldBe Right(Some(1234))
      optionalCurrency("error.currency").bind(Map("" -> "99999999999")) shouldBe Right(Some(BigDecimal("99999999999")))
    }

    "report an invalid money error" in {
      optionalCurrency("error.currency").bind(Map("" -> "£")) shouldBe Left(List(FormError("", "error.currency")))
      optionalCurrency("error.currency").bind(Map("" -> "a")) shouldBe Left(List(FormError("", "error.currency")))
      optionalCurrency("error.currency").bind(Map("" -> "1234.001")) shouldBe Left(List(FormError("", "error.currency")))
      optionalCurrency("error.currency").bind(Map("" -> "1.234.001")) shouldBe Left(List(FormError("", "error.currency")))
      optionalCurrency("error.currency").bind(Map("" -> "99999999999999999")) shouldBe Left(List(FormError("", "error.currency")))
      optionalCurrency("error.currency").bind(Map("" -> "0.00")) shouldBe Left(List(FormError("", "error.currency")))
      optionalCurrency("error.currency").bind(Map("" -> "1234.01")) shouldBe Left(List(FormError("", "error.currency")))
      optionalCurrency("error.currency").bind(Map("" -> "1234.1")) shouldBe Left(List(FormError("", "error.currency")))
      optionalCurrency("error.currency").bind(Map("" -> "-1234")) shouldBe Left(List(FormError("", "error.currency")))
    }

    "report correctly for blank value" in {
      optionalCurrency("error.currency").bind(Map("" -> "")) shouldBe Right(None)
    }
  }

}