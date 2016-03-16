package models.declaration

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError

class RoleWithinBusinessSpec extends PlaySpec with MockitoSugar {

  "When the user inputs the data that is posted in the form, the role within business" must {

    "successfully pass validation for Beneficial Shareholder" in {
      val urlFormEncoded = Map("roleWithinBusiness" -> Seq("01"))
      RoleWithinBusiness.formRule.validate(urlFormEncoded) must be(Success(BeneficialShareholder))
    }

    "successfully pass validation for Director" in {
      val urlFormEncoded = Map("roleWithinBusiness" -> Seq("02"))
      RoleWithinBusiness.formRule.validate(urlFormEncoded) must be(Success(Director))
    }

    "successfully pass validation for External Accountant" in {
      val urlFormEncoded = Map("roleWithinBusiness" -> Seq("03"))
      RoleWithinBusiness.formRule.validate(urlFormEncoded) must be(Success(ExternalAccountant))
    }

    "successfully pass validation for Internal Accountant" in {
      val urlFormEncoded = Map("roleWithinBusiness" -> Seq("04"))
      RoleWithinBusiness.formRule.validate(urlFormEncoded) must be(Success(InternalAccountant))
    }

    "successfully pass validation for Nominated Officer" in {
      val urlFormEncoded = Map("roleWithinBusiness" -> Seq("05"))
      RoleWithinBusiness.formRule.validate(urlFormEncoded) must be(Success(NominatedOfficer))
    }

    "successfully pass validation for Partner" in {
      val urlFormEncoded = Map("roleWithinBusiness" -> Seq("06"))
      RoleWithinBusiness.formRule.validate(urlFormEncoded) must be(Success(Partner))
    }

    "successfully pass validation for Sole Proprietor" in {
      val urlFormEncoded = Map("roleWithinBusiness" -> Seq("07"))
      RoleWithinBusiness.formRule.validate(urlFormEncoded) must be(Success(SoleProprietor))
    }

    "successfully pass validation for Other value" in {
      val urlFormEncoded = Map(
        "roleWithinBusiness" -> Seq("08"),
        "other" -> Seq("Some other value")
      )
      RoleWithinBusiness.formRule.validate(urlFormEncoded) must be(Success(Other("Some other value")))
    }

    "fail validation if not Other value" in {
      val urlFormEncoded = Map(
        "roleWithinBusiness" -> Seq("08"),
        "other" -> Seq("")
      )
      RoleWithinBusiness.formRule.validate(urlFormEncoded) must be(Failure(Seq(
        (Path \ "other") -> Seq(ValidationError("error.required"))
      )))
    }

    "fail to validate given an invalid value supplied that is not matching to any role" in {

      val urlFormEncoded = Map("roleWithinBusiness" -> Seq("10"))

      RoleWithinBusiness.formRule.validate(urlFormEncoded) must
        be(Failure(Seq(
          (Path \ "roleWithinBusiness") -> Seq(ValidationError("error.invalid"))
        )))
    }
  }

  "When the user loads the page and that is posted to the form, the role within business" must {

    "load the correct value in the form for Beneficial Shareholder" in {
      RoleWithinBusiness.formWrite.writes(BeneficialShareholder) must be(Map("roleWithinBusiness" -> Seq("01")))
    }

    "load the correct value in the form for Director" in {
      RoleWithinBusiness.formWrite.writes(Director) must be(Map("roleWithinBusiness" -> Seq("02")))
    }

    "load the correct value in the form for ExternalAccountant" in {
      RoleWithinBusiness.formWrite.writes(ExternalAccountant) must be(Map("roleWithinBusiness" -> Seq("03")))
    }

    "load the correct value in the form for InternalAccountant" in {
      RoleWithinBusiness.formWrite.writes(InternalAccountant) must be(Map("roleWithinBusiness" -> Seq("04")))
    }

    "load the correct value in the form for NominatedOfficer" in {
      RoleWithinBusiness.formWrite.writes(NominatedOfficer) must be(Map("roleWithinBusiness" -> Seq("05")))
    }

    "load the correct value in the form for Partner" in {
      RoleWithinBusiness.formWrite.writes(Partner) must be(Map("roleWithinBusiness" -> Seq("06")))
    }

    "load the correct value in the form for SoleProprietor" in {
      RoleWithinBusiness.formWrite.writes(SoleProprietor) must be(Map("roleWithinBusiness" -> Seq("07")))
    }

    "load the correct value in the form for Other value" in {
      RoleWithinBusiness.formWrite.writes(Other("some value")) must be(Map(
        "roleWithinBusiness" -> Seq("08"),
        "other" -> Seq("some value")
      ))
    }


  }

}
