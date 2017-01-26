package models.aboutthebusiness

import cats.data.Validated.{Invalid, Valid}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.Path
import jto.validation.ValidationError

class ConfirmRegisteredOfficeSpec extends PlaySpec with MockitoSugar {
  "RegOfficeOrMainPlaceOfBusiness" must {

    "successfully validate" when {
      "given a 'true' value" in {

        val data = Map(
          "isRegOfficeOrMainPlaceOfBusiness" -> Seq("true")
        )

        ConfirmRegisteredOffice.formRule.validate(data) must
          be(Valid(ConfirmRegisteredOffice(true)))
      }

      "given a 'false' value" in {

        val data = Map(
          "isRegOfficeOrMainPlaceOfBusiness" -> Seq("false")
        )

        ConfirmRegisteredOffice.formRule.validate(data) must
          be(Valid(ConfirmRegisteredOffice(false)))
      }
    }

    "fail validation" when {
      "given missing data represented by an empty Map" in {

        ConfirmRegisteredOffice.formRule.validate(Map.empty) must
          be(Invalid(Seq(
            (Path \ "isRegOfficeOrMainPlaceOfBusiness") -> Seq(ValidationError("error.required.atb.confirm.office"))
          )))
      }

      "given missing data represented by an empty string" in {

        val data = Map(
          "isRegOfficeOrMainPlaceOfBusiness" -> Seq("")
        )

        ConfirmRegisteredOffice.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "isRegOfficeOrMainPlaceOfBusiness") -> Seq(ValidationError("error.required.atb.confirm.office"))
          )))
      }
    }

    "write correct data" in {

      val model = ConfirmRegisteredOffice(true)

      ConfirmRegisteredOffice.formWrites.writes(model) must
        be(Map(
          "isRegOfficeOrMainPlaceOfBusiness" -> Seq("true")
        ))
    }
  }
}
