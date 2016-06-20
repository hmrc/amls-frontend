package models.moneyservicebusiness

import models.Country
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json.{JsSuccess, Json}

class MostTransactionsSpec extends PlaySpec {

  "MostTransactions" must {

    val rule: Rule[UrlFormEncoded, MostTransactions] = implicitly
    val write: Write[MostTransactions, UrlFormEncoded] = implicitly

    "roundtrip through json" in {

      val model: MostTransactions =
        MostTransactions(Seq(Country("United Kingdom", "GB")))

      Json.fromJson[MostTransactions](Json.toJson(model)) mustEqual JsSuccess(model)
    }

    "roundtrip through forms" in {

      val model: MostTransactions =
        MostTransactions(Seq(Country("United Kingdom", "GB")))

      rule.validate(write.writes(model)) mustEqual Success(model)
    }

    "fail to validate when there are no countries" in {

      val form: UrlFormEncoded = Map(
        "mostTransactionsCountries" -> Seq.empty
      )

      rule.validate(form) mustEqual Failure(
        Seq((Path \ "mostTransactionsCountries") -> Seq(ValidationError("error.required.countries.msb.most.transactions")))
      )
    }

    "fail to validate when there are more than 3 countries" in {

      // scalastyle:off magic.number
      val form: UrlFormEncoded = Map(
        "mostTransactionsCountries[]" -> Seq.fill(4)("GB")
      )

      rule.validate(form) mustEqual Failure(
        Seq((Path \ "mostTransactionsCountries") -> Seq(ValidationError("error.maxLength", 3)))
      )
    }
  }
}
