package models.moneyservicebusiness

import cats.data.Validated.Valid
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.FakeApplication

class WhichCurrenciesPreRelease7Spec extends PlaySpec with OneAppPerSuite {

  override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.services.feature-toggle.release7" -> false))

  "WhichCurrencies" must {

    val model = WhichCurrencies(
      Seq("USD", "CHF", "EUR"),
      None,
      Some(BankMoneySource("Bank names")),
      Some(WholesalerMoneySource("wholesaler names")),
      customerMoneySource = Some(true))

    "pass form validation" when {

      "no foreign currency flag is set" in {

        val formData = Map(
          "currencies[0]" -> Seq("USD"),
          "currencies[1]" -> Seq("CHF"),
          "currencies[2]" -> Seq("EUR"),
          "bankMoneySource" -> Seq("Yes"),
          "bankNames" -> Seq("Bank names"),
          "wholesalerMoneySource" -> Seq("Yes"),
          "wholesalerNames" -> Seq("wholesaler names"),
          "customerMoneySource" -> Seq("Yes")
        )

        WhichCurrencies.formR.validate(formData) must be(Valid(model))

      }

    }

    "serialize the json properly" when {
      "no foreign currency flag is set" in {
        Json.toJson(model).as[WhichCurrencies] must be(model)
      }
    }

  }

}
