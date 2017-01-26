package models.moneyservicebusiness

import org.scalatest.{MustMatchers, WordSpec}
import jto.validation.{Path, Invalid, Valid}
import jto.validation.forms.UrlFormEncoded
import jto.validation.ValidationError
import play.api.libs.json.Json

class WhichCurrenciesSpec extends WordSpec with MustMatchers {
  "Which Currencies" when  {
    "data is complete" should {
      val fullModel = WhichCurrencies(Seq("USD", "CHF", "EUR"), Some(BankMoneySource("Bank names")), Some(WholesalerMoneySource("wholesaler names")), true)
      val fullFormData = Map(
        "currencies[0]" -> Seq("USD"),
        "currencies[1]" -> Seq("CHF"),
        "currencies[2]" -> Seq("EUR"),
        "bankMoneySource" -> Seq("Yes"),
        "bankNames" -> Seq("Bank names"),
        "wholesalerMoneySource" -> Seq("Yes"),
        "wholesalerNames" -> Seq("wholesaler names"),
        "customerMoneySource" -> Seq("Yes")
      )

      "Write correctly to a form" in {
        WhichCurrencies.formW.writes(fullModel) must be (fullFormData)
      }

      "Read correctly from a form" in {
        WhichCurrencies.formR.validate(fullFormData) must be(Valid(fullModel))
      }

      "Round trip through Json correctly" in {
        Json.toJson(fullModel).as[WhichCurrencies] must be(fullModel)
      }
    }

    "there is no bankMoneySource" should {
      val model = WhichCurrencies(Seq("USD", "CHF", "EUR"), None, Some(WholesalerMoneySource("wholesaler names")), true)
      val formData = Map(
        "currencies[0]" -> Seq("USD"),
        "currencies[1]" -> Seq("CHF"),
        "currencies[2]" -> Seq("EUR"),
        "wholesalerMoneySource" -> Seq("Yes"),
        "wholesalerNames" -> Seq("wholesaler names"),
        "customerMoneySource" -> Seq("Yes")
      )

      "Write correctly to a form" in {
        WhichCurrencies.formW.writes(model) must be (formData)
      }

      "Read correctly from a form" in {
        WhichCurrencies.formR.validate(formData) must be(Valid(model))
      }

      "Round trip through Json correctly" in {
        Json.toJson(model).as[WhichCurrencies] must be(model)
      }
    }

    def buildString(length: Int, acc : String = ""): String = {
      length match {
        case 0 => ""
        case 1 => "X"
        case _ => "X" ++ buildString(length - 1)
      }
    }

    "bank money source is checked and BankNames is empty" should {
      val formData = Map(
        "currencies[0]" -> Seq("USD"),
        "currencies[1]" -> Seq("CHF"),
        "currencies[2]" -> Seq("EUR"),
        "bankMoneySource" -> Seq("Yes"),
        "bankNames" -> Seq("")
      )

      "fail validation" in {
        WhichCurrencies.formR.validate(formData) must be (Invalid(Seq((Path \ "bankNames") -> Seq(ValidationError("error.invalid.msb.wc.bankNames")))))
      }
    }

    "wholesaler money source is checked and wholesaler Names is empty" should {
      val formData = Map(
        "currencies[0]" -> Seq("USD"),
        "currencies[1]" -> Seq("CHF"),
        "currencies[2]" -> Seq("EUR"),
        "wholesalerMoneySource" -> Seq("Yes"),
        "wholesalerNames" -> Seq("")
      )

      "fail validation" in {
        WhichCurrencies.formR.validate(formData) must be (Invalid(Seq((Path \ "wholesalerNames") -> Seq(ValidationError("error.invalid.msb.wc.wholesalerNames")))))
      }
    }

    "BankNames > 140 characters" should {
      val formData = Map(
        "currencies[0]" -> Seq("USD"),
        "currencies[1]" -> Seq("CHF"),
        "currencies[2]" -> Seq("EUR"),
        "bankMoneySource" -> Seq("Yes"),
        "bankNames" -> Seq(buildString(141)),
        "wholesalerMoneySource" -> Seq("Yes"),
        "wholesalerNames" -> Seq("wholesaler names"),
        "customerMoneySource" -> Seq("Yes")
      )

      "fail validation " in {
        WhichCurrencies.formR.validate(formData) must be (Invalid(Seq((Path \ "bankNames") -> Seq(ValidationError("error.invalid.msb.wc.bankNames.too-long")))))
      }
    }

    "currencies are sent as blank strings" should {
      val formData = Map(
        "currencies[0]" -> Seq(""),
        "currencies[1]" -> Seq(""),
        "currencies[2]" -> Seq(""),
        "bankMoneySource" -> Seq("Yes"),
        "bankNames" -> Seq("Bank names"),
        "wholesalerMoneySource" -> Seq("Yes"),
        "wholesalerNames" -> Seq("wholesaler names"),
        "customerMoneySource" -> Seq("Yes")
      )

      "fail validation" in {
        WhichCurrencies.formR.validate(formData) must be (Invalid(Seq((Path \ "currencies") -> Seq(ValidationError("error.invalid.msb.wc.currencies")))))
      }
    }

    "WholesalerNames > 140 characters" should {
      val formData = Map(
        "currencies[0]" -> Seq("USD"),
        "currencies[1]" -> Seq("CHF"),
        "currencies[2]" -> Seq("EUR"),
        "bankMoneySource" -> Seq("Yes"),
        "bankNames" -> Seq("Nak names"),
        "wholesalerMoneySource" -> Seq("Yes"),
        "wholesalerNames" -> Seq(buildString(141)),
        "customerMoneySource" -> Seq("Yes")
      )

      "fail validation " in {
        WhichCurrencies.formR.validate(formData) must be (Invalid(Seq((Path \ "wholesalerNames") -> Seq(ValidationError("error.invalid.msb.wc.wholesalerNames.too-long")))))
      }
    }

    "No money source specified" should {
      "fail validation" in {
        val formData = Map(
          "currencies[0]" -> Seq("USD"),
          "currencies[1]" -> Seq("CHF"),
          "currencies[2]" -> Seq("EUR")
        )

        WhichCurrencies.formR.validate(formData) must be (Invalid(Seq((Path \ "WhoWillSupply") -> Seq(ValidationError("error.invalid.msb.wc.moneySources")))))
      }
    }

    "No currencies entered" should {
      "fail validation" in{
        val formData = Map(
          "bankMoneySource" -> Seq("Yes"),
          "bankNames" -> Seq("Bank names"),
          "wholesalerMoneySource" -> Seq("Yes"),
          "wholesalerNames" -> Seq("wholesaler names"),
          "customerMoneySource" -> Seq("Yes")
        )

        WhichCurrencies.formR.validate(formData) must be (Invalid(Seq((Path \ "currencies") -> Seq(ValidationError("error.invalid.msb.wc.currencies")))))
      }
    }

    "No currencies or sources entered" should {
      val formData = Map(
        "currencies[0]" -> Seq(""),
        "currencies[1]" -> Seq(""),
        "currencies[2]" -> Seq(""),
        "bankNames" -> Seq(""),
        "wholesalerNames" -> Seq("")
      )

      "fail validation with both error messages" in {
        WhichCurrencies.formR.validate(formData) must be (Invalid(Seq(
          (Path \ "currencies") -> Seq(ValidationError("error.invalid.msb.wc.currencies")),
          (Path \ "WhoWillSupply") -> Seq(ValidationError("error.invalid.msb.wc.moneySources"))
        )))
      }
    }

    "No currencies entered and bankName Missing" should {
      val formData = Map(
        "currencies[0]" -> Seq(""),
        "currencies[1]" -> Seq(""),
        "currencies[2]" -> Seq(""),
        "bankMoneySource" -> Seq("Yes"),
        "bankNames" -> Seq(""),
        "wholesalerNames" -> Seq("")
      )

      "fail validation with both error messages" in {
        WhichCurrencies.formR.validate(formData) must be (Invalid(Seq(
          (Path \ "currencies") -> Seq(ValidationError("error.invalid.msb.wc.currencies")),
          (Path \ "bankNames") -> Seq(ValidationError("error.invalid.msb.wc.bankNames"))
        )))
      }
    }
  }
}
