package models.aboutthebusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}
import play.api.data.validation.ValidationError

class ConfirmRegisteredOfficeSpec extends PlaySpec with MockitoSugar {
  "RegOfficeOrMainPlaceOfBusiness" must {

    "validate the given model" in {
      val data = Map(
        "isRegOfficeOrMainPlaceOfBusiness" -> Seq("true")
      )

      ConfirmRegisteredOffice.formRule.validate(data) must
        be(Success(ConfirmRegisteredOffice(true)))
    }

    "successfully validate given a data model" in {

      val data = Map(
        "isRegOfficeOrMainPlaceOfBusiness" -> Seq("true")
      )

      ConfirmRegisteredOffice.formRule.validate(data) must
        be(Success(ConfirmRegisteredOffice(true)))
    }

    "fail to validate when given invalid data" in {

      ConfirmRegisteredOffice.formRule.validate(Map.empty) must
        be(Failure(Seq(
          (Path \ "isRegOfficeOrMainPlaceOfBusiness") -> Seq(ValidationError("error.required.atb.confirm.office"))
        )))
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
