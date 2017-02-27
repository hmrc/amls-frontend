package models.declaration.release7

import jto.validation.{Invalid, Path, Valid, ValidationError}
import models.declaration.RoleWithinBusiness
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}
import play.api.test.FakeApplication


class RoleWithinBusinessRelease7Spec extends PlaySpec with MockitoSugar with OneAppPerSuite{


  override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.services.feature-toggle.release7" -> true))

  "RoleWithinBusiness" must {

    "validate model with few check box selected" in {

      val model = Map(
        "roleWithinBusiness[]" -> Seq("BeneficialShareholder", "Director" ,"Other"),
        "roleWithinBusinessOther" -> Seq("test")
      )

      RoleWithinBusinessRelease7.formRule.validate(model) must
        be(Valid(RoleWithinBusinessRelease7(Set(
          BeneficialShareholder,
          Director,
          Other("test")
        ))))

    }

    "fail validation when 'Other' is selected but no details are provided" when {
      "represented by an empty string" in {
        val model = Map("roleWithinBusiness[]" -> Seq("Other"),
          "roleWithinBusinessOther" -> Seq(""))

        RoleWithinBusinessRelease7.formRule.validate(model) must
          be(Invalid(List((Path \ "roleWithinBusinessOther", Seq(ValidationError("error.required.declaration.specify.role"))))))
      }

      "represented by a sequence of whitespace" in {
        val model = Map("roleWithinBusiness[]" -> Seq("Other"),
          "roleWithinBusinessOther" -> Seq("  \t"))

        RoleWithinBusinessRelease7.formRule.validate(model) must
          be(Invalid(List((Path \ "roleWithinBusinessOther", Seq(ValidationError("error.required.declaration.specify.role"))))))
      }

      "represented by a missing field" in {
        val model = Map("roleWithinBusiness[]" -> Seq("Other"))
        RoleWithinBusinessRelease7.formRule.validate(model) must
          be(Invalid(List((Path \ "roleWithinBusinessOther", Seq(ValidationError("error.required"))))))
      }
    }

    "fail validation when field otherDetails exceeds maximum length" in {

      val model = Map(
        "roleWithinBusiness[]" -> Seq(
          "BeneficialShareholder",
          "Director",
          "Partner",
          "InternalAccountant",
          "SoleProprietor",
          "Other"),
        "roleWithinBusinessOther" -> Seq("t"*256)
      )
      RoleWithinBusinessRelease7.formRule.validate(model) must
        be(Invalid(List(( Path \ "roleWithinBusinessOther", Seq(ValidationError("error.invalid.maxlength.255"))))))
    }


    "fail validation when none of the check boxes are selected" when {
      List(
        "empty list" -> Map("roleWithinBusiness[]" -> Seq(),"roleWithinBusinessOther" -> Seq("test")),
        "missing field" -> Map.empty[String, Seq[String]]
      ).foreach { x =>
        val (rep, model) = x
        s"represented by $rep" in {
          RoleWithinBusinessRelease7.formRule.validate(model) must
            be(Invalid(List((Path \ "roleWithinBusiness", List(ValidationError("error.required"))))))
        }
      }
    }

    "fail to validate invalid data" in {
      val model = Map(
        "roleWithinBusiness[]" -> Seq("BeneficialShareholder, dfdfdfdf")
      )
      RoleWithinBusinessRelease7.formRule.validate(model) must
        be(Invalid(Seq((Path \ "roleWithinBusiness") -> Seq(ValidationError("error.invalid")))))

    }

    "validate form write for valid transaction record" in {

      val map = Map(
        "roleWithinBusiness[]" -> Seq("Other","Director"),
        "roleWithinBusinessOther" -> Seq("test")
      )

      val model = RoleWithinBusinessRelease7(Set(Other("test"), Director))
      RoleWithinBusinessRelease7.formWrites.writes(model) must be (map)
    }

    "validate form write for multiple options" in {

      val map = Map(
        "roleWithinBusiness[]" -> Seq("Partner", "SoleProprietor","DesignatedMember","BeneficialShareholder","ExternalAccountant")
      )

      val model = RoleWithinBusinessRelease7(Set(BeneficialShareholder, SoleProprietor, Partner, DesignatedMember, ExternalAccountant))
      RoleWithinBusinessRelease7.formWrites.writes(model) must be (map)
    }

    "JSON validation" must {

      "successfully convert release 6 data to release 7 model" in {
        val json =  Json.obj(
          "roleWithinBusiness" -> "01")

        Json.fromJson[RoleWithinBusinessRelease7](json) must
          be(JsSuccess(RoleWithinBusinessRelease7(Set(BeneficialShareholder)), JsPath))
      }

      "successfully validate given values" in {
        val json =  Json.obj(
          "roleWithinBusiness" -> Seq("SoleProprietor","NominatedOfficer", "DesignatedMember", "Director", "BeneficialShareholder"))

        Json.fromJson[RoleWithinBusinessRelease7](json) must
          be(JsSuccess(RoleWithinBusinessRelease7(Set(SoleProprietor, NominatedOfficer, DesignatedMember, Director, BeneficialShareholder)), JsPath))
      }
      "successfully validate given all values" in {
        val json =  Json.obj(
          "roleWithinBusiness" -> Seq("BeneficialShareholder","Director","Partner","InternalAccountant","ExternalAccountant",
            "SoleProprietor","NominatedOfficer","DesignatedMember", "Other"),
        "roleWithinBusinessOther" -> "some other text")



        Json.fromJson[RoleWithinBusinessRelease7](json) must
          be(JsSuccess(RoleWithinBusinessRelease7(Set(
            BeneficialShareholder,
            Director,
            Partner,
            InternalAccountant,
            ExternalAccountant,
            SoleProprietor,
            NominatedOfficer,
            DesignatedMember,
            Other("some other text"))), JsPath))
      }

      "successfully validate given values with option other details" in {
        val json =  Json.obj(
          "roleWithinBusiness" -> Seq("DesignatedMember", "Other"),
          "roleWithinBusinessOther" -> "test")

        Json.fromJson[RoleWithinBusinessRelease7](json) must
          be(JsSuccess(RoleWithinBusinessRelease7(Set(Other("test"), DesignatedMember)), JsPath))
      }

      "fail when path is missing" in {
        Json.fromJson[RoleWithinBusinessRelease7](Json.obj(
          "roleWithinBusinessOther" -> "other text")) must
          be(JsError((JsPath \ "roleWithinBusiness") -> play.api.data.validation.ValidationError("error.path.missing")))
      }

      "fail when on invalid data" in {
        Json.fromJson[RoleWithinBusinessRelease7](Json.obj("roleWithinBusiness" -> Set("hello"))) must
          be(JsError((JsPath \ "roleWithinBusiness") -> play.api.data.validation.ValidationError("error.invalid")))
      }

      "write valid data in using json write" in {
        val release = RoleWithinBusinessRelease7(Set(SoleProprietor, Other("test657")))

        Json.toJson[RoleWithinBusinessRelease7](release) must be (
          Json.obj("roleWithinBusiness" -> Json.arr("SoleProprietor", "Other"),
            "roleWithinBusinessOther" -> "test657"
          ))
      }
    }
  }

  "otherDetailsType" must {
    "pass validation" when {
      "255 valid characters are entered" in {
        RoleWithinBusinessRelease7.otherDetailsType.validate("1" * 255) must be(Valid("1" * 255))
      }
    }

    "fail validation" when {
      "more than 255 characters are entered" in {
        RoleWithinBusinessRelease7.otherDetailsType.validate("1" * 256) must be(
          Invalid(Seq(Path -> Seq(ValidationError("error.invalid.maxlength.255")))))
      }
      "given an empty string" in {
        RoleWithinBusinessRelease7.otherDetailsType.validate("") must be(
          Invalid(Seq(Path -> Seq(ValidationError("error.required.declaration.specify.role")))))
      }
      "given whitespace only" in {
        RoleWithinBusinessRelease7.otherDetailsType.validate("     ") must be(
          Invalid(Seq(Path -> Seq(ValidationError("error.required.declaration.specify.role")))))
      }
      "given invalid characters" in {
        RoleWithinBusinessRelease7.otherDetailsType.validate("{}") must be(
          Invalid(Seq(Path -> Seq(ValidationError("err.text.validation")))))
      }
    }
  }
}

