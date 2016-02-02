package models.aboutyou

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class RoleWithinBusinessSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "successfully validate given an enum value" in {

      RoleWithinBusiness.formRule.validate(Map("roleWithinBusiness" -> Seq("01"))) must
        be(Success(BeneficialShareholder))

      RoleWithinBusiness.formRule.validate(Map("roleWithinBusiness" -> Seq("02"))) must
        be(Success(Director))

      RoleWithinBusiness.formRule.validate(Map("roleWithinBusiness" -> Seq("03"))) must
        be(Success(ExternalAccountant))

      RoleWithinBusiness.formRule.validate(Map("roleWithinBusiness" -> Seq("04"))) must
        be(Success(InternalAccountant))

      RoleWithinBusiness.formRule.validate(Map("roleWithinBusiness" -> Seq("05"))) must
        be(Success(NominatedOfficer))

      RoleWithinBusiness.formRule.validate(Map("roleWithinBusiness" -> Seq("06"))) must
        be(Success(Partner))

      RoleWithinBusiness.formRule.validate(Map("roleWithinBusiness" -> Seq("07"))) must
        be(Success(SoleProprietor))
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

    "write correct data from enum value" in {

      RoleWithinBusiness.formWrites.writes(BeneficialShareholder) must
        be(Map("roleWithinBusiness" -> Seq("01")))

      RoleWithinBusiness.formWrites.writes(Director) must
        be(Map("roleWithinBusiness" -> Seq("02")))

      RoleWithinBusiness.formWrites.writes(ExternalAccountant) must
        be(Map("roleWithinBusiness" -> Seq("03")))

      RoleWithinBusiness.formWrites.writes(InternalAccountant) must
        be(Map("roleWithinBusiness" -> Seq("04")))

      RoleWithinBusiness.formWrites.writes(NominatedOfficer) must
        be(Map("roleWithinBusiness" -> Seq("05")))

      RoleWithinBusiness.formWrites.writes(Partner) must
        be(Map("roleWithinBusiness" -> Seq("06")))

      RoleWithinBusiness.formWrites.writes(SoleProprietor) must
        be(Map("roleWithinBusiness" -> Seq("07")))
    }

    "write correct data from `other` value" in {

      RoleWithinBusiness.formWrites.writes(Other("foobar")) must
        be(Map(
          "roleWithinBusiness" -> Seq("08"),
          "other" -> Seq("foobar")
        ))
    }
  }

  "JSON validation" must {

    "successfully validate given an enum value" in {

      Json.fromJson[RoleWithinBusiness](Json.obj("roleWithinBusiness" -> "01")) must
        be(JsSuccess(BeneficialShareholder, JsPath \ "roleWithinBusiness"))

      Json.fromJson[RoleWithinBusiness](Json.obj("roleWithinBusiness" -> "02")) must
        be(JsSuccess(Director, JsPath \ "roleWithinBusiness"))

      Json.fromJson[RoleWithinBusiness](Json.obj("roleWithinBusiness" -> "03")) must
        be(JsSuccess(ExternalAccountant, JsPath \ "roleWithinBusiness"))

      Json.fromJson[RoleWithinBusiness](Json.obj("roleWithinBusiness" -> "04")) must
        be(JsSuccess(InternalAccountant, JsPath \ "roleWithinBusiness"))

      Json.fromJson[RoleWithinBusiness](Json.obj("roleWithinBusiness" -> "05")) must
        be(JsSuccess(NominatedOfficer, JsPath \ "roleWithinBusiness"))

      Json.fromJson[RoleWithinBusiness](Json.obj("roleWithinBusiness" -> "06")) must
        be(JsSuccess(Partner, JsPath \ "roleWithinBusiness"))

      Json.fromJson[RoleWithinBusiness](Json.obj("roleWithinBusiness" -> "07")) must
        be(JsSuccess(SoleProprietor, JsPath \ "roleWithinBusiness"))
    }

    "successfully validate given an `other` value" in {

      val json = Json.obj(
        "roleWithinBusiness" -> "08",
        "roleWithinBusinessOther" -> "foo"
      )

      Json.fromJson[RoleWithinBusiness](json) must
        be(JsSuccess(Other("foo"), JsPath \ "roleWithinBusiness" \ "roleWithinBusinessOther"))
    }

    "fail to validate when given an invalid enum value" in {

      val json = Json.obj(
      )

      Json.fromJson[RoleWithinBusiness](json) must
        be(JsError((JsPath \ "roleWithinBusiness") -> ValidationError("error.invalid")))
    }

    "fail to validate when given an empty `other` value" in {

      val json = Json.obj(
        "roleWithinBusiness" -> "08"
      )

      Json.fromJson[RoleWithinBusiness](json) must
        be(JsError((JsPath \ "roleWithinBusiness" \ "roleWithinBusinessOther") -> ValidationError("error.path.missing")))
    }

    "write the correct value" in {

      Json.toJson(BeneficialShareholder) must
        be(Json.obj("roleWithinBusiness" -> "01"))

      Json.toJson(Director) must
        be(Json.obj("roleWithinBusiness" -> "02"))

      Json.toJson(ExternalAccountant) must
        be(Json.obj("roleWithinBusiness" -> "03"))

      Json.toJson(InternalAccountant) must
        be(Json.obj("roleWithinBusiness" -> "04"))

      Json.toJson(NominatedOfficer) must
        be(Json.obj("roleWithinBusiness" -> "05"))

      Json.toJson(Partner) must
        be(Json.obj("roleWithinBusiness" -> "06"))

      Json.toJson(SoleProprietor) must
        be(Json.obj("roleWithinBusiness" -> "07"))

      Json.toJson(Other("foobar")) must
        be(Json.obj(
          "roleWithinBusiness" -> "08",
          "roleWithinBusinessOther" -> "foobar"
        ))
    }
  }
}
