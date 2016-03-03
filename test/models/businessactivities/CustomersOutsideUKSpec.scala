package models.businessactivities

import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}
import play.api.data.validation.ValidationError

class CustomersOutsideUKSpec extends PlaySpec {


  "CustomersOutsideUK" must {
    "successfully validate the form Rule with option No" in {
      CustomersOutsideUK.formRule.validate(Map("isOutside" -> Seq("false"))) must
        be(Success(CustomersOutsideUKNo))
    }

    "successfully validate the form Rule with option Yes" in {
      CustomersOutsideUK.formRule.validate(Map("isOutside" -> Seq("true"),
      "country_1" -> Seq("GS"))) must
        be(Success(CustomersOutsideUKYes(Countries("GS"))))
    }

    "validate mandatory field when isOutside is not selected" in {
      CustomersOutsideUK.formRule.validate(Map.empty) must
        be(Failure(Seq(
          (Path \ "isOutside") -> Seq(ValidationError("error.required"))
        )))
    }

    "validate mandatory field when isOutside is  selected as Yes and country with invalid data" in {
      val json = Map("isOutside" -> Seq("true"),
      "country_1" -> Seq("ABC"))

      CustomersOutsideUK.formRule.validate(json) must
        be(Failure(Seq(
          (Path \ "country_1") -> Seq(ValidationError("error.maxLength", 2))
        )))
    }

    "validate mandatory country field" in {
      CustomersOutsideUK.formRule.validate(Map("isOutside" -> Seq("true"))) must
        be(Failure(Seq(
          (Path \ "country_1") -> Seq(ValidationError("error.required"))
        )))
    }
  }
}
