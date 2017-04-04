package models.responsiblepeople

import cats.data.Validated.{Invalid, Valid}
import jto.validation.{Path, ValidationError}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec

class ConfirmAddressSpec extends PlaySpec with MockitoSugar {
  
  "ConfirmAddress" must {

    "successfully validate" when {
      "given a 'true' value" in {

        val data = Map(
          "confirmAddress" -> Seq("true")
        )

        ConfirmAddress.formRule.validate(data) must
          be(Valid(ConfirmAddress(true)))
      }

      "given a 'false' value" in {

        val data = Map(
          "confirmAddress" -> Seq("false")
        )

        ConfirmAddress.formRule.validate(data) must
          be(Valid(ConfirmAddress(false)))
      }
    }

    "fail validation" when {
      "given missing data represented by an empty Map" in {

        ConfirmAddress.formRule.validate(Map.empty) must
          be(Invalid(Seq(
            (Path \ "confirmAddress") -> Seq(ValidationError("error.required.rp.confirm.address"))
          )))
      }

      "given missing data represented by an empty string" in {

        val data = Map(
          "confirmAddress" -> Seq("")
        )

        ConfirmAddress.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "confirmAddress") -> Seq(ValidationError("error.required.rp.confirm.address"))
          )))
      }
    }

    "write correct data" in {

      val model = ConfirmAddress(true)

      ConfirmAddress.formWrites.writes(model) must
        be(Map(
          "confirmAddress" -> Seq("true")
        ))
    }
  }
}
