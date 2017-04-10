package models.renewal

import jto.validation.{Invalid, Path, Valid, ValidationError}
import models.Country
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsSuccess

class SendTheLargestAmountsOfMoneySpec extends PlaySpec {


  "SendTheLargestAmountsOfMoney" must {

    "successfully validate the form Rule with option Yes" in {
      MsbSendTheLargestAmountsOfMoney.formRule.validate(
        Map(
          "country_1" -> Seq("AL"),
          "country_2" -> Seq("DZ"),
          "country_3" -> Seq("AS")
        )
      ) mustBe {
        Valid(MsbSendTheLargestAmountsOfMoney(
          
            Country("Albania", "AL"),
            Some(Country("Algeria", "DZ")),
            Some(Country("American Samoa", "AS"))
          )
        )
      }
    }


    "validate mandatory field when isOutside is  selected as Yes and country with invalid data" in {
      val json = Map("country_1" -> Seq("ABC"))

      MsbSendTheLargestAmountsOfMoney.formRule.validate(json) must
        be(Invalid(Seq(
          (Path \ "country_1") -> Seq(ValidationError("error.required.renewal.country.name"))
        )))
    }

    "validate mandatory field for min length when isOutside is  selected as Yes and country with invalid data" in {
      val json = Map("country_1" -> Seq("A"))

      MsbSendTheLargestAmountsOfMoney.formRule.validate(json) must
        be(Invalid(Seq(
          (Path \ "country_1") -> Seq(ValidationError("error.required.renewal.country.name"))
        )))
    }

    "validate mandatory country field" in {
      MsbSendTheLargestAmountsOfMoney.formRule.validate(Map("country_1" -> Seq(""))) must
        be(Invalid(Seq(
          (Path \ "country_1") -> Seq(ValidationError("error.required.renewal.country.name"))
        )))
    }

    "successfully write model with formWrite" in {

      val model = MsbSendTheLargestAmountsOfMoney(Country("United Kingdom", "GB"), Some(Country("United Kingdom", "GB")))
      MsbSendTheLargestAmountsOfMoney.formWrites.writes(model) must
        contain allOf (
        "country_1" -> Seq("GB"),
        "country_2" -> Seq("GB")
        )
    }

    "JSON validation" must {
      "successfully validate givcen values" in {
        val data = MsbSendTheLargestAmountsOfMoney(Country("United Kingdom", "GB"))

        MsbSendTheLargestAmountsOfMoney.format.reads(MsbSendTheLargestAmountsOfMoney.format.writes(data)) must
          be(JsSuccess(data))
      }
     }
  }
}
