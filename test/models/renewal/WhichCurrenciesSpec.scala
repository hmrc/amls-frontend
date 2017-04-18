package models.renewal

import jto.validation.{Invalid, Path, Valid, ValidationError}
import models.CharacterSets
import models.moneyservicebusiness.{BankMoneySource, WholesalerMoneySource}
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.libs.json.Json
import play.api.test.FakeApplication

class WhichCurrenciesSpec extends WordSpec with MustMatchers with OneAppPerSuite with CharacterSets {

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
        WhichCurrencies.formR.validate(fullFormData) must be(Valid(fullModel))
      }

      "Round trip through Json correctly" in {
        val js = Json.toJson(fullModel)
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

        WhichCurrencies.formR.validate(form) must be(Invalid(Seq(Path \ "usesForeignCurrencies" -> Seq(ValidationError("error.required.msb.wc.foreignCurrencies")))))

      }

      "infer the value of usesForeignCurrencies correctly" when {
        "deserializing from JSON and there is no value set" in {

          val json = Json.toJson(WhichCurrencies(Seq("GBP", "USD"), None, Some(BankMoneySource("Some bank")), None, None))
          val result = json.as[WhichCurrencies]

          result.usesForeignCurrencies must be(Some(true))
        }

        "deserializing from JSON and there is some value set" in {
          val json = Json.toJson(WhichCurrencies(Seq("GBP", "USD"), Some(false), Some(BankMoneySource("Some bank")), None, None))
          val result = json.as[WhichCurrencies]

          result.usesForeignCurrencies must be(Some(false))
        }
      }

    }

    "there is no foreignCurrency flag but contains foreign currency form data" should {

      val fullFormData = Map(
        "currencies[0]" -> Seq("USD"),
        "currencies[1]" -> Seq("CHF"),
        "currencies[2]" -> Seq("EUR"),
        "bankMoneySource" -> Seq("Yes"),
        "bankNames" -> Seq("Bank names"),
        "usesForeignCurrencies" -> Seq("Yes")
      )

      val model = WhichCurrencies(Seq("USD", "CHF", "EUR"), None, Some(BankMoneySource("Bank names")), None, None)

      "set usesForeignCurrencies to true" in {

        WhichCurrencies.formW.writes(model) must be(fullFormData)

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
        WhichCurrencies.formR.validate(formData) must be(Valid(model))
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
        WhichCurrencies.formR.validate(formData) must be(Invalid(Seq((Path \ "bankNames") -> Seq(ValidationError("error.invalid.renewal.msb.wc.bankNames")))))
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
        WhichCurrencies.formR.validate(formData) must be(Invalid(Seq((Path \ "wholesalerNames") -> Seq(ValidationError("error.invalid.renewal.msb.wc.wholesalerNames")))))
      }
    }

    "bankNames and wholesalerNames > 140 characters" should {
      val formData = Map(
        "currencies[0]" -> Seq("USD"),
        "currencies[1]" -> Seq("CHF"),
        "currencies[2]" -> Seq("EUR"),
        "bankMoneySource" -> Seq("Yes"),
        "bankNames" -> Seq(buildString(141)),
        "wholesalerMoneySource" -> Seq("Yes"),
        "wholesalerNames" -> Seq(buildString(141)),
        "customerMoneySource" -> Seq("Yes"),
        "usesForeignCurrencies" -> Seq("Yes")
      )

      "fail validation " in {
        WhichCurrencies.formR.validate(formData) must be(Invalid(Seq(
          (Path \ "bankNames") -> Seq(ValidationError("error.invalid.maxlength.140")),
          (Path \ "wholesalerNames") -> Seq(ValidationError("error.invalid.maxlength.140"))
        )))
      }
    }

    "bankNames and wholesalerNames contain symbols outside the Trading Names pattern" should {

      val formData = Map(
        "currencies[0]" -> Seq("USD"),
        "currencies[1]" -> Seq("CHF"),
        "currencies[2]" -> Seq("EUR"),
        "bankMoneySource" -> Seq("Yes"),
        "bankNames" -> Seq(symbols5.mkString("")),
        "wholesalerMoneySource" -> Seq("Yes"),
        "wholesalerNames" -> Seq(symbols5.mkString("")),
        "customerMoneySource" -> Seq("Yes"),
        "usesForeignCurrencies" -> Seq("Yes")
      )

      "fail validation" in {
        WhichCurrencies.formR.validate(formData) must be(Invalid(Seq(
          (Path \ "bankNames") -> Seq(ValidationError("err.text.validation")),
          (Path \ "wholesalerNames") -> Seq(ValidationError("err.text.validation"))
        )))
      }

    }

    "bankNames and wholesalerNames contain whitespace only" should {

      val formData = Map(
        "currencies[0]" -> Seq("USD"),
        "currencies[1]" -> Seq("CHF"),
        "currencies[2]" -> Seq("EUR"),
        "bankMoneySource" -> Seq("Yes"),
        "bankNames" -> Seq("   "),
        "wholesalerMoneySource" -> Seq("Yes"),
        "wholesalerNames" -> Seq("   "),
        "customerMoneySource" -> Seq("Yes"),
        "usesForeignCurrencies" -> Seq("Yes")
      )

      "fail validation" in {
        WhichCurrencies.formR.validate(formData) must be(Invalid(Seq(
          (Path \ "bankNames") -> Seq(ValidationError("error.invalid.renewal.msb.wc.bankNames")),
          (Path \ "wholesalerNames") -> Seq(ValidationError("error.invalid.renewal.msb.wc.wholesalerNames"))
        )))
      }

    }

    "bankNames and wholesalerNames contain standard UK alpha characters" should {

      val alpha = (alphaLower.take(4) ++ alphaUpper.take(4)).mkString("")

      val formData = Map(
        "currencies[0]" -> Seq("USD"),
        "currencies[1]" -> Seq("CHF"),
        "currencies[2]" -> Seq("EUR"),
        "bankMoneySource" -> Seq("Yes"),
        "bankNames" -> Seq(alpha),
        "wholesalerMoneySource" -> Seq("Yes"),
        "wholesalerNames" -> Seq(alpha),
        "customerMoneySource" -> Seq("Yes"),
        "usesForeignCurrencies" -> Seq("Yes")
      )

      "pass validation" in {
        WhichCurrencies.formR.validate(formData) must be(Valid(WhichCurrencies(List("USD", "CHF", "EUR"),Some(true),Some(BankMoneySource(alpha)),Some(WholesalerMoneySource(alpha)),Some(true))))
      }

    }

    "bankNames and wholesalerNames contain accented characters" should {

      val accentedAlpha = (extendedAlphaLower.take(4) ++ extendedAlphaUpper.take(4)).mkString("")

      val formData = Map(
        "currencies[0]" -> Seq("USD"),
        "currencies[1]" -> Seq("CHF"),
        "currencies[2]" -> Seq("EUR"),
        "bankMoneySource" -> Seq("Yes"),
        "bankNames" -> Seq(accentedAlpha),
        "wholesalerMoneySource" -> Seq("Yes"),
        "wholesalerNames" -> Seq(accentedAlpha),
        "customerMoneySource" -> Seq("Yes"),
        "usesForeignCurrencies" -> Seq("Yes")
      )

      "pass validation" in {
        WhichCurrencies.formR.validate(formData) must be(Valid(WhichCurrencies(List("USD", "CHF", "EUR"),Some(true),Some(BankMoneySource(accentedAlpha)),Some(WholesalerMoneySource(accentedAlpha)),Some(true))))
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
        WhichCurrencies.formR.validate(formData) must be(Invalid(Seq((Path \ "currencies") -> Seq(ValidationError("error.invalid.msb.wc.currencies")))))
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

        WhichCurrencies.formR.validate(formData) must be(Invalid(Seq((Path \ "WhoWillSupply") -> Seq(ValidationError("error.invalid.renewal.msb.wc.moneySources")))))
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

        WhichCurrencies.formR.validate(formData) must be(Invalid(Seq((Path \ "currencies") -> Seq(ValidationError("error.invalid.msb.wc.currencies")))))
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
        WhichCurrencies.formR.validate(formData) must be(Invalid(Seq(
          (Path \ "currencies") -> Seq(ValidationError("error.invalid.msb.wc.currencies")),
          (Path \ "WhoWillSupply") -> Seq(ValidationError("error.invalid.renewal.msb.wc.moneySources"))
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
        WhichCurrencies.formR.validate(formData) must be(Invalid(Seq(
          (Path \ "currencies") -> Seq(ValidationError("error.invalid.msb.wc.currencies")),
          (Path \ "bankNames") -> Seq(ValidationError("error.invalid.renewal.msb.wc.bankNames"))
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

        WhichCurrencies.formR.validate(formData) must be(Valid(model))

      }

    }
  }
}
