package models.aboutthebusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}
import play.api.data.validation.ValidationError

class ConfirmRegisteredOfficeOrMainPlaceOfBusinessSpec extends PlaySpec with MockitoSugar {
  "RegOfficeOrMainPlaceOfBusiness " must {

    "validate the given model " in {
      val data = Map(
        "isRegOfficeOrMainPlaceOfBusiness" -> Seq("true")
      )

      ConfirmRegisteredOfficeOrMainPlaceOfBusiness.formRule.validate(data) must
        be(Success(ConfirmRegisteredOfficeOrMainPlaceOfBusiness(true)))
    }

    "successfully validate given a data model" in {

      val data = Map(
        "isRegOfficeOrMainPlaceOfBusiness" -> Seq("true")
      )

      ConfirmRegisteredOfficeOrMainPlaceOfBusiness.formRule.validate(data) must
        be(Success(ConfirmRegisteredOfficeOrMainPlaceOfBusiness(true)))
    }

    "fail to validate when given invalid data" in {

      ConfirmRegisteredOfficeOrMainPlaceOfBusiness.formRule.validate(Map.empty) must
        be(Failure(Seq(
          (Path \ "isRegOfficeOrMainPlaceOfBusiness") -> Seq(ValidationError("error.required"))
        )))
    }

    "write correct data" in {

      val model = ConfirmRegisteredOfficeOrMainPlaceOfBusiness(true)

      ConfirmRegisteredOfficeOrMainPlaceOfBusiness.formWrites.writes(model) must
        be(Map(
          "isRegOfficeOrMainPlaceOfBusiness" -> Seq("true")
        ))
    }
  }
}
