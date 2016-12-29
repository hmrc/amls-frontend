package models.aboutthebusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError

class ConfirmRegisteredOfficeSpec extends PlaySpec with MockitoSugar {
  "RegOfficeOrMainPlaceOfBusiness" must {

    "successfully validate" when {
      "given a 'true' value" in {

        val data = Map(
          "isRegOfficeOrMainPlaceOfBusiness" -> Seq("true")
        )

        ConfirmRegisteredOffice.formRule.validate(data) must
          be(Success(ConfirmRegisteredOffice(true)))
      }
    }

    "fail validation" when {
      "given missing data represented by an empty Map" in {

        ConfirmRegisteredOffice.formRule.validate(Map.empty) must
          be(Failure(Seq(
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
