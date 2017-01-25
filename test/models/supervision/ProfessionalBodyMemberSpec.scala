package models.supervision

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Failure, Path, Success}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}


class ProfessionalBodyMemberSpec extends PlaySpec with MockitoSugar {

  "ProfessionalBodyMember" must {

    "validate model with few check box selected" in {

      val model = Map(
        "isAMember" -> Seq("true"),
        "businessType[]" -> Seq("01", "02" ,"14"),
        "specifyOtherBusiness" -> Seq("test")
      )

      ProfessionalBodyMember.formRule.validate(model) must
        be(Success(ProfessionalBodyMemberYes(Set(AccountingTechnicians, CharteredCertifiedAccountants, Other("test")))))

    }

    "validate model with option No selected" in {

      val model = Map(
        "isAMember" -> Seq("false")
      )

      ProfessionalBodyMember.formRule.validate(model) must
        be(Success(ProfessionalBodyMemberNo))

    }

    "fail validation when field member of professional not selected" in {

      val model = Map(
        "businessType[]" -> Seq("01", "02" ,"03"),
        "specifyOtherBusiness" -> Seq("")
      )

      ProfessionalBodyMember.formRule.validate(model) must
        be(Failure(List(( Path \ "isAMember", Seq(ValidationError("error.required.supervision.business.a.member"))))))

    }

    "fail validation when field is business a member of professional body selected and specifyOtherBusiness is empty" in {

      val model = Map(
        "isAMember" -> Seq("true"),
        "businessType[]" -> Seq("01", "02" ,"14"),
        "specifyOtherBusiness" -> Seq("")
      )
      ProfessionalBodyMember.formRule.validate(model) must
        be(Failure(List(( Path \ "specifyOtherBusiness", Seq(ValidationError("error.required.supervision.business.details"))))))
    }

    "fail validation when field is business a member of professional body selected and specifyOther exceeds max length" in {

      val model = Map(
        "isAMember" -> Seq("true"),
        "businessType[]" -> Seq("01", "02" ,"14"),
        "specifyOtherBusiness" -> Seq("test"*200)
      )
      ProfessionalBodyMember.formRule.validate(model) must
        be(Failure(List(( Path \ "specifyOtherBusiness", Seq(ValidationError("error.invalid.supervision.business.details"))))))
    }

    "fail validation when none of the check boxes selected" in {

      val model = Map(
        "isAMember" -> Seq("true"),
        "businessType[]" -> Seq(),
        "specifyOtherBusiness" -> Seq("test")
      )
      ProfessionalBodyMember.formRule.validate(model) must
        be(Failure(List(( Path \ "businessType", Seq(ValidationError("error.required.supervision.one.professional.body"))))))
    }

    "fail to validate on empty Map" in {

      ProfessionalBodyMember.formRule.validate(Map.empty) must
        be(Failure(Seq((Path \ "isAMember") -> Seq(ValidationError("error.required.supervision.business.a.member")))))

    }

    "fail to validate invalid data" in {

      val model = Map(
        "isAMember" -> Seq("true"),
        "businessType[]" -> Seq("01", "20")
      )
      ProfessionalBodyMember.formRule.validate(model) must
        be(Failure(Seq((Path \ "businessType") -> Seq(ValidationError("error.invalid")))))

    }

    "validate form write for valid transaction record" in {

      val map = Map(
        "isAMember" -> Seq("true"),
        "businessType[]" -> Seq("14","13"),
        "specifyOtherBusiness" -> Seq("test")
      )

      val model = ProfessionalBodyMemberYes(Set(Other("test"), LawSociety))

     ProfessionalBodyMember.formWrites.writes(model) must be (map)
    }

    "validate form write for option No" in {

      val map = Map(
        "isAMember" -> Seq("false")
      )
      val model = ProfessionalBodyMemberNo
      ProfessionalBodyMember.formWrites.writes(model) must be (map)
    }

    "validate form write for option Yes" in {

      val map = Map(
        "isAMember" -> Seq("true"),
        "businessType[]" -> Seq("03","04", "05", "06")
      )

      val model = ProfessionalBodyMemberYes(Set(InternationalAccountants,
        TaxationTechnicians, ManagementAccountants, InstituteOfTaxation))
      ProfessionalBodyMember.formWrites.writes(model) must be (map)
    }

    "form write test" in {
      val map = Map(
        "isAMember" -> Seq("false")
      )
      val model = ProfessionalBodyMemberNo

      ProfessionalBodyMember.formWrites.writes(model) must be(map)
    }

    "JSON validation" must {

      "successfully validate given values" in {
        val json =  Json.obj("isAMember" -> true,
          "businessType" -> Seq("01","02"))

        Json.fromJson[ProfessionalBodyMember](json) must
          be(JsSuccess(ProfessionalBodyMemberYes(Set(AccountingTechnicians, CharteredCertifiedAccountants)), JsPath \ "isAMember" \ "businessType"))
      }

      "successfully validate given values with option No" in {
        val json =  Json.obj("isAMember" -> false)

        Json.fromJson[ProfessionalBodyMember](json) must
          be(JsSuccess(ProfessionalBodyMemberNo, JsPath \ "isAMember"))
      }

      "successfully validate given values with option Digital software" in {
        val json =  Json.obj("isAMember" -> true,
          "businessType" -> Seq("14", "12"),
        "specifyOtherBusiness" -> "test")

        Json.fromJson[ProfessionalBodyMember](json) must
          be(JsSuccess(ProfessionalBodyMemberYes(Set(Other("test"), AssociationOfBookkeepers)), JsPath \ "isAMember" \ "businessType" \ "specifyOtherBusiness"))
      }

      "fail when on path is missing" in {
        Json.fromJson[ProfessionalBodyMember](Json.obj("isAMember" -> true,
          "transaction" -> Seq("01"))) must
          be(JsError((JsPath \ "isAMember" \ "businessType") -> ValidationError("error.path.missing")))
      }

      "fail when on invalid data" in {
        Json.fromJson[ProfessionalBodyMember](Json.obj("isAMember" -> true,"businessType" -> Seq("40"))) must
          be(JsError(((JsPath \ "isAMember" \ "businessType") \ "businessType") -> ValidationError("error.invalid")))
      }

      "write valid data in using json write" in {
        Json.toJson[ProfessionalBodyMember](ProfessionalBodyMemberYes(Set(AccountantsScotland, Other("test657")))) must be (Json.obj("isAMember" -> true,
        "businessType" -> Seq("09", "14"),
          "specifyOtherBusiness" -> "test657"
        ))
      }

      "write valid data in using json write with Option No" in {
        Json.toJson[ProfessionalBodyMember](ProfessionalBodyMemberNo) must be (Json.obj("isAMember" -> false))
      }
    }
  }
}


