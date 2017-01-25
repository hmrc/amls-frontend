package models.moneyservicebusiness

import org.scalatestplus.play.PlaySpec
import jto.validation.{Failure, Path, Success}
import jto.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess}

class SendMoneyToOtherCountrySpec extends PlaySpec {

  "SendMoneyToOtherCountry" should {

    "Form Validation" must {

      "Successfully read form data for option yes" in {

        val map = Map("money" -> Seq("true"))

        SendMoneyToOtherCountry.formRule.validate(map) must be(Success(SendMoneyToOtherCountry(true)))
      }

      "Successfully read form data for option no" in {

        val map = Map("money" -> Seq("false"))

        SendMoneyToOtherCountry.formRule.validate(map) must be(Success(SendMoneyToOtherCountry(false)))
      }

      "fail validation on missing field" in {

        SendMoneyToOtherCountry.formRule.validate(Map.empty) must be(Failure(
          Seq( Path \ "money" -> Seq(ValidationError("error.required.msb.send.money")))))
      }

      "successfully write form data" in {

        SendMoneyToOtherCountry.formWrites.writes(SendMoneyToOtherCountry(false)) must be(Map("money" -> Seq("false")))
      }
    }

    "Json Validation" must {

      "Successfully read/write Json data" in {

        SendMoneyToOtherCountry.format.reads(SendMoneyToOtherCountry.format.writes(
          SendMoneyToOtherCountry(false))) must be(JsSuccess(SendMoneyToOtherCountry(false), JsPath \ "money"))

      }
    }
  }
}
