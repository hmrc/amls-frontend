package models.aboutyou

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}
import play.api.data.validation.ValidationError

class RoleWithinBusinessSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "successfully validate given an enum value" in {

      val data = Map(
        "roleWithinBusiness" -> Seq("01")
      )

      RoleWithinBusiness.formRule.validate(data) must
        be(Success(BeneficialShareholder))
    }

    "successfully validate given an `other` value" in {

      val data = Map(
        "roleWithinBusiness" -> Seq("08"),
        "other" -> Seq("foobar")
      )

      RoleWithinBusiness.formRule.validate(data) must
        be(Success(Other("foobar")))
    }

    "fail to validate given an `other` with no value" in {

      val data = Map(
        "roleWithinBusiness" -> Seq("08")
      )

      RoleWithinBusiness.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "other") -> Seq(ValidationError("error.required"))
        )))
    }

    "fail to validate given a non-enum value" in {

      val data = Map(
        "roleWithinBusiness" -> Seq("10")
      )

      RoleWithinBusiness.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "roleWithinBusiness") -> Seq(ValidationError("error.invalid"))
        )))
    }

    "fail to validate given an empty value" in {

      RoleWithinBusiness.formRule.validate(Map.empty) must
        be(Failure(Seq(
          (Path \ "roleWithinBusiness") -> Seq(ValidationError("error.required"))
        )))
    }

    "write correct data from enum value" in {

      RoleWithinBusiness.formWrites.writes(BeneficialShareholder) must
        be(Map(
          "roleWithinBusiness" -> Seq("01")
        ))
    }

    "write correct data from `other` value" in {

      RoleWithinBusiness.formWrites.writes(Other("foobar")) must
        be(Map(
          "roleWithinBusiness" -> Seq("08"),
          "other" -> Seq("foobar")
        ))
    }
  }
}
