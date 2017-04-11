package models.renewal

import jto.validation.{ValidationError, _}
import jto.validation.forms.UrlFormEncoded
import models.Country
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}

class MostTransactionsSpec extends PlaySpec {

  "MostTransactions" must {

    val rule: Rule[UrlFormEncoded, MsbMostTransactions] = implicitly
    val write: Write[MsbMostTransactions, UrlFormEncoded] = implicitly

    "roundtrip through json" in {

      val model: MsbMostTransactions =
        MsbMostTransactions(Seq(Country("United Kingdom", "GB")))

      Json.fromJson[MsbMostTransactions](Json.toJson(model)) mustEqual JsSuccess(model)
    }

    "roundtrip through forms" in {

      val model: MsbMostTransactions =
        MsbMostTransactions(Seq(Country("United Kingdom", "GB")))

      rule.validate(write.writes(model)) mustEqual Valid(model)
    }

    "fail to validate when there are no countries" in {

      val form: UrlFormEncoded = Map(
        "mostTransactionsCountries" -> Seq.empty
      )

      rule.validate(form) mustEqual Invalid(
        Seq((Path \ "mostTransactionsCountries") -> Seq(ValidationError("error.required.renewal.country.name")))
      )
    }

    "fail to validate when there are more than 3 countries" in {

      // scalastyle:off magic.number
      val form: UrlFormEncoded = Map(
        "mostTransactionsCountries[]" -> Seq.fill(4)("GB")
      )

      rule.validate(form) mustEqual Invalid(
        Seq((Path \ "mostTransactionsCountries") -> Seq(ValidationError("error.maxLength", 3)))
      )
    }
  }

  "MostTransactions Form Writes" when {
    "an item is repeated" must {
      "serialise all items correctly" in {
        MsbMostTransactions.formW.writes(MsbMostTransactions(List(
          Country("Country2", "BB"),
          Country("Country1", "AA"),
          Country("Country1", "AA")
        ))) must be (
          Map(
            "mostTransactionsCountries[0]" -> List("BB"),
            "mostTransactionsCountries[1]" -> List("AA"),
            "mostTransactionsCountries[2]" -> List("AA")
          )
        )
      }
    }
  }

  "Most Transactions Form Reads" when {
    "all countries are valid" must {
      "Successfully read from the form" in {

        MsbMostTransactions.formR.validate(
          Map(
            "mostTransactionsCountries[0]" -> Seq("GB"),
            "mostTransactionsCountries[1]" -> Seq("MK"),
            "mostTransactionsCountries[2]" -> Seq("JO")
          )
        ) must be(Valid(MsbMostTransactions(Seq(
          Country("United Kingdom", "GB"),
          Country("Macedonia, the Former Yugoslav Republic of", "MK"),
          Country("Jordan", "JO")
        ))))
      }
    }

    "the second country is invalid" must {
      "fail validation" in {

        val x: VA[MsbMostTransactions] = MsbMostTransactions.formR.validate(
          Map(
            "mostTransactionsCountries[0]" -> Seq("GB"),
            "mostTransactionsCountries[1]" -> Seq("hjjkhjkjh"),
            "mostTransactionsCountries[2]" -> Seq("MK")
          )
        )
        x must be (Invalid(Seq((Path \ "mostTransactionsCountries" \ 1) -> Seq(ValidationError("error.invalid.country")))))
      }
    }
  }
}
