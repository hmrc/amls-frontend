package models.moneyservicebusiness

import config.ApplicationConfig
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.Json
import play.api.test.FakeApplication

class WhichCurrenciesSpec extends WordSpec with MustMatchers with OneAppPerSuite {

  override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.services.feature-toggle.release7" -> true))

  "Which Currencies" when {
    "data is complete" should {

      val fullModel = WhichCurrencies(
        Seq("USD", "CHF", "EUR"),
        usesForeignCurrencies = Some(true),
        Some(BankMoneySource("Bank names")),
        Some(WholesalerMoneySource("wholesaler names")),
        customerMoneySource = Some(true))

      val fullFormData = Map(
        "currencies[0]" -> Seq("USD"),
        "currencies[1]" -> Seq("CHF"),
        "currencies[2]" -> Seq("EUR"),
        "bankMoneySource" -> Seq("Yes"),
        "bankNames" -> Seq("Bank names"),
        "wholesalerMoneySource" -> Seq("Yes"),
        "wholesalerNames" -> Seq("wholesaler names"),
        "customerMoneySource" -> Seq("Yes"),
        "usesForeignCurrencies" -> Seq("Yes")
      )

      "Write correctly to a form" in {
        WhichCurrencies.formW.writes(fullModel) must be(fullFormData)
      }

      "Read correctly from a form" in {
        println(ApplicationConfig.release7)
        WhichCurrencies.formR.validate(fullFormData) must be(Success(fullModel))
      }

      "Round trip through Json correctly" in {
        val js = Json.toJson(fullModel)
        println(Console.YELLOW + js + Console.WHITE)
        js.as[WhichCurrencies] must be(fullModel)
      }
    }

    "there is no foreignCurrency flag present" should {

      val form = Map(
        "currencies[0]" -> Seq("USD"),
        "currencies[0]" -> Seq("USD"),
        "currencies[0]" -> Seq("USD")
      )

      "fail validation" in {

        WhichCurrencies.formR.validate(form) must be(Failure(Seq(Path \ "usesForeignCurrencies" -> Seq(ValidationError("error.required.msb.wc.foreignCurrencies")))))

      }

    }

    "there is no bankMoneySource" should {

      val model = WhichCurrencies(
        Seq("USD", "CHF", "EUR"),
        usesForeignCurrencies = Some(true),
        None,
        Some(WholesalerMoneySource("wholesaler names")),
        customerMoneySource = Some(true))

      val formData = Map(
        "currencies[0]" -> Seq("USD"),
        "currencies[1]" -> Seq("CHF"),
        "currencies[2]" -> Seq("EUR"),
        "wholesalerMoneySource" -> Seq("Yes"),
        "wholesalerNames" -> Seq("wholesaler names"),
        "customerMoneySource" -> Seq("Yes"),
        "usesForeignCurrencies" -> Seq("Yes")
      )

      "Write correctly to a form" in {
        WhichCurrencies.formW.writes(model) must be(formData)
      }

      "Read correctly from a form" in {
        WhichCurrencies.formR.validate(formData) must be(Success(model))
      }

      "Round trip through Json correctly" in {
        val json = Json.toJson(model)
        json.as[WhichCurrencies] must be(model)
      }
    }

    def buildString(length: Int, acc: String = ""): String = {
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
        "bankNames" -> Seq(""),
        "usesForeignCurrencies" -> Seq("Yes")
      )

      "fail validation" in {
        WhichCurrencies.formR.validate(formData) must be(Failure(Seq((Path \ "bankNames") -> Seq(ValidationError("error.invalid.msb.wc.bankNames")))))
      }
    }

    "wholesaler money source is checked and wholesaler Names is empty" should {
      val formData = Map(
        "currencies[0]" -> Seq("USD"),
        "currencies[1]" -> Seq("CHF"),
        "currencies[2]" -> Seq("EUR"),
        "wholesalerMoneySource" -> Seq("Yes"),
        "wholesalerNames" -> Seq(""),
        "usesForeignCurrencies" -> Seq("Yes")
      )

      "fail validation" in {
        WhichCurrencies.formR.validate(formData) must be(Failure(Seq((Path \ "wholesalerNames") -> Seq(ValidationError("error.invalid.msb.wc.wholesalerNames")))))
      }
    }

    "bankNames > 140 characters" should {
      val formData = Map(
        "currencies[0]" -> Seq("USD"),
        "currencies[1]" -> Seq("CHF"),
        "currencies[2]" -> Seq("EUR"),
        "bankMoneySource" -> Seq("Yes"),
        "bankNames" -> Seq(buildString(141)),
        "wholesalerMoneySource" -> Seq("Yes"),
        "wholesalerNames" -> Seq("wholesaler names"),
        "customerMoneySource" -> Seq("Yes"),
        "usesForeignCurrencies" -> Seq("Yes")
      )

      "fail validation " in {
        WhichCurrencies.formR.validate(formData) must be(Failure(Seq((Path \ "bankNames") -> Seq(ValidationError("error.invalid.msb.wc.bankNames.too-long")))))
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
        "customerMoneySource" -> Seq("Yes"),
        "usesForeignCurrencies" -> Seq("Yes")
      )

      "fail validation" in {
        WhichCurrencies.formR.validate(formData) must be(Failure(Seq((Path \ "currencies") -> Seq(ValidationError("error.invalid.msb.wc.currencies")))))
      }
    }

    "wholesalerNames > 140 characters" should {
      val formData = Map(
        "currencies[0]" -> Seq("USD"),
        "currencies[1]" -> Seq("CHF"),
        "currencies[2]" -> Seq("EUR"),
        "bankMoneySource" -> Seq("Yes"),
        "bankNames" -> Seq("Nak names"),
        "wholesalerMoneySource" -> Seq("Yes"),
        "wholesalerNames" -> Seq(buildString(141)),
        "customerMoneySource" -> Seq("Yes"),
        "usesForeignCurrencies" -> Seq("Yes")
      )

      "fail validation " in {
        WhichCurrencies.formR.validate(formData) must be(Failure(Seq((Path \ "wholesalerNames") -> Seq(ValidationError("error.invalid.msb.wc.wholesalerNames.too-long")))))
      }
    }

    "no money source specified" should {
      "fail validation" in {
        val formData = Map(
          "currencies[0]" -> Seq("USD"),
          "currencies[1]" -> Seq("CHF"),
          "currencies[2]" -> Seq("EUR"),
          "usesForeignCurrencies" -> Seq("Yes")
        )

        WhichCurrencies.formR.validate(formData) must be(Failure(Seq((Path \ "WhoWillSupply") -> Seq(ValidationError("error.invalid.msb.wc.moneySources")))))
      }
    }

    "no currencies entered" should {
      "fail validation" in {
        val formData = Map(
          "bankMoneySource" -> Seq("Yes"),
          "bankNames" -> Seq("Bank names"),
          "wholesalerMoneySource" -> Seq("Yes"),
          "wholesalerNames" -> Seq("wholesaler names"),
          "customerMoneySource" -> Seq("Yes"),
          "usesForeignCurrencies" -> Seq("Yes")
        )

        WhichCurrencies.formR.validate(formData) must be(Failure(Seq((Path \ "currencies") -> Seq(ValidationError("error.invalid.msb.wc.currencies")))))
      }
    }

    "no currencies or sources entered" should {
      val formData = Map(
        "currencies[0]" -> Seq(""),
        "currencies[1]" -> Seq(""),
        "currencies[2]" -> Seq(""),
        "bankNames" -> Seq(""),
        "wholesalerNames" -> Seq(""),
        "usesForeignCurrencies" -> Seq("Yes")
      )

      "fail validation with error messages" in {
        WhichCurrencies.formR.validate(formData) must be(Failure(Seq(
          (Path \ "currencies") -> Seq(ValidationError("error.invalid.msb.wc.currencies")),
          (Path \ "WhoWillSupply") -> Seq(ValidationError("error.invalid.msb.wc.moneySources"))
        )))
      }
    }

    "no currencies entered and bankName Missing" should {
      val formData = Map(
        "currencies[0]" -> Seq(""),
        "currencies[1]" -> Seq(""),
        "currencies[2]" -> Seq(""),
        "bankMoneySource" -> Seq("Yes"),
        "bankNames" -> Seq(""),
        "wholesalerNames" -> Seq(""),
        "usesForeignCurrencies" -> Seq("Yes")
      )

      "fail validation with both error messages" in {
        WhichCurrencies.formR.validate(formData) must be(Failure(Seq(
          (Path \ "currencies") -> Seq(ValidationError("error.invalid.msb.wc.currencies")),
          (Path \ "bankNames") -> Seq(ValidationError("error.invalid.msb.wc.bankNames"))
        )))
      }
    }

    "the business does not deal in foreign currencies" should {

      val formData = Map(
        "currencies[0]" -> Seq("GBP"),
        "currencies[1]" -> Seq("EUR"),
        "currencies[2]" -> Seq("USD"),
        "usesForeignCurrencies" -> Seq("No")
      )

      "not fail validation for the foreign currency fields" in {

        val model = WhichCurrencies(Seq("GBP", "EUR", "USD"), usesForeignCurrencies = Some(false), None, None, None)

        WhichCurrencies.formR.validate(formData) must be(Success(model))

      }

    }
  }
}
