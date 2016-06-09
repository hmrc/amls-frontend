package models.moneyservicebusiness

import org.scalatest.{MustMatchers, WordSpec}
import play.api.data.mapping.Success
import play.api.data.mapping.forms.UrlFormEncoded
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
        WhichCurrencies.formR.validate(fullFormData) must be(Success(fullModel))
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
        WhichCurrencies.formR.validate(formData) must be(Success(model))
      }

      "Round trip through Json correctly" in {
        Json.toJson(model).as[WhichCurrencies] must be(model)
      }
    }
  }
}
