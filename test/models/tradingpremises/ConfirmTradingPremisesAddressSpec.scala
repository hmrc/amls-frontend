package models.tradingpremises

import cats.data.Validated.{Invalid, Valid}
import jto.validation.{Path, ValidationError}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec

class ConfirmTradingPremisesAddressSpec extends PlaySpec with MockitoSugar {
  
  "ConfirmTradingPremisesAddress" must {

    "successfully validate" when {
      "given a 'true' value" in {

        val data = Map(
          "confirmAddress" -> Seq("true")
        )

        ConfirmTradingPremisesAddress.formRule.validate(data) must
          be(Valid(ConfirmTradingPremisesAddress(true)))
      }

      "given a 'false' value" in {

        val data = Map(
          "confirmAddress" -> Seq("false")
        )

        ConfirmTradingPremisesAddress.formRule.validate(data) must
          be(Valid(ConfirmTradingPremisesAddress(false)))
      }
    }

    "fail validation" when {
      "given missing data represented by an empty Map" in {

        ConfirmTradingPremisesAddress.formRule.validate(Map.empty) must
          be(Invalid(Seq(
            (Path \ "confirmAddress") -> Seq(ValidationError("error.required.tp.confirm.address"))
          )))
      }

      "given missing data represented by an empty string" in {

        val data = Map(
          "confirmAddress" -> Seq("")
        )

        ConfirmTradingPremisesAddress.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "confirmAddress") -> Seq(ValidationError("error.required.tp.confirm.address"))
          )))
      }
    }

    "write correct data" in {

      val model = ConfirmTradingPremisesAddress(true)

      ConfirmTradingPremisesAddress.formWrites.writes(model) must
        be(Map(
          "confirmAddress" -> Seq("true")
        ))
    }
  }
}
