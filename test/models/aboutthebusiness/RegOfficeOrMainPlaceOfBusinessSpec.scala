package models.aboutthebusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}
import play.api.data.validation.ValidationError

class RegOfficeOrMainPlaceOfBusinessSpec extends PlaySpec with MockitoSugar {
  "RegOfficeOrMainPlaceOfBusiness " must {

    "validate the given model " in {
      val data = Map(
        "isRegOfficeOrMainPlaceOfBusiness" -> Seq("true")
      )

      RegOfficeOrMainPlaceOfBusiness.formRule.validate(data) must
        be(Success(RegOfficeOrMainPlaceOfBusiness(true)))
    }

    "successfully validate given a data model" in {

      val data = Map(
        "isRegOfficeOrMainPlaceOfBusiness" -> Seq("true")
      )

      RegOfficeOrMainPlaceOfBusiness.formRule.validate(data) must
        be(Success(RegOfficeOrMainPlaceOfBusiness(true)))
    }

    "fail to validate when given invalid data" in {

      RegOfficeOrMainPlaceOfBusiness.formRule.validate(Map.empty) must
        be(Failure(Seq(
          (Path \ "isRegOfficeOrMainPlaceOfBusiness") -> Seq(ValidationError("error.required"))
        )))
    }

    "write correct data" in {

      val model = RegOfficeOrMainPlaceOfBusiness(true)

      RegOfficeOrMainPlaceOfBusiness.formWrites.writes(model) must
        be(Map(
          "isRegOfficeOrMainPlaceOfBusiness" -> Seq("true")
        ))
    }
  }
}
