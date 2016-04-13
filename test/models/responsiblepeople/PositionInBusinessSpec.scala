package models.responsiblepeople

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class PositionInBusinessSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "PositionInBusiness" must {

      "successfully validate" in {
        PositionWithinBusiness.formRule.validate(Map("positionWithinBusiness" -> Seq("01"))) must be(Success(BeneficialOwner))
        PositionWithinBusiness.formRule.validate(Map("positionWithinBusiness" -> Seq("02"))) must be(Success(Director))
        PositionWithinBusiness.formRule.validate(Map("positionWithinBusiness" -> Seq("03"))) must be(Success(InternalAccountant))
        PositionWithinBusiness.formRule.validate(Map("positionWithinBusiness" -> Seq("04"))) must be(Success(NominatedOfficer))
        PositionWithinBusiness.formRule.validate(Map("positionWithinBusiness" -> Seq("05"))) must be(Success(Partner))
        PositionWithinBusiness.formRule.validate(Map("positionWithinBusiness" -> Seq("06"))) must be(Success(SoleProprietor))
      }

      "fail to validate an empty string" in {
        PositionWithinBusiness.formRule.validate(Map("positionWithinBusiness" -> Seq(""))) must
          be(Failure(Seq(
            (Path \ "positionWithinBusiness") -> Seq(ValidationError("error.required.positionWithinBusiness"))
          )))
      }

    }

    "write correct data from enum value" in {
      PositionWithinBusiness.formWrite.writes(BeneficialOwner) must be(Map("positionWithinBusiness" -> Seq("01")))
      PositionWithinBusiness.formWrite.writes(Director) must be(Map("positionWithinBusiness" -> Seq("02")))
      PositionWithinBusiness.formWrite.writes(InternalAccountant) must be(Map("positionWithinBusiness" -> Seq("03")))
      PositionWithinBusiness.formWrite.writes(NominatedOfficer) must be(Map("positionWithinBusiness" -> Seq("04")))
      PositionWithinBusiness.formWrite.writes(Partner) must be(Map("positionWithinBusiness" -> Seq("05")))
      PositionWithinBusiness.formWrite.writes(SoleProprietor) must be(Map("positionWithinBusiness" -> Seq("06")))
    }

  }

  "JSON validation" must {

    "successfully validate given a BeneficialOwner value" in {
      Json.fromJson[PositionWithinBusiness](Json.obj("positionWithinBusiness" -> "01")) must
        be(JsSuccess(BeneficialOwner, JsPath \ "positionWithinBusiness"))
    }

    "successfully validate given a Director value" in {
      Json.fromJson[PositionWithinBusiness](Json.obj("positionWithinBusiness" -> "02")) must
        be(JsSuccess(Director, JsPath \ "positionWithinBusiness"))
    }

    "successfully validate given a InternalAccountant value" in {
      Json.fromJson[PositionWithinBusiness](Json.obj("positionWithinBusiness" -> "03")) must
        be(JsSuccess(InternalAccountant, JsPath \ "positionWithinBusiness"))
    }

    "successfully validate given a NominatedOfficer value" in {
      Json.fromJson[PositionWithinBusiness](Json.obj("positionWithinBusiness" -> "04")) must
        be(JsSuccess(NominatedOfficer, JsPath \ "positionWithinBusiness"))
    }

    "successfully validate given a Partner value" in {
      Json.fromJson[PositionWithinBusiness](Json.obj("positionWithinBusiness" -> "05")) must
        be(JsSuccess(Partner, JsPath \ "positionWithinBusiness"))
    }

    "successfully validate given a SoleProprietor value" in {
      Json.fromJson[PositionWithinBusiness](Json.obj("positionWithinBusiness" -> "06")) must
        be(JsSuccess(SoleProprietor, JsPath \ "positionWithinBusiness"))
    }

    "fail to validate when given an empty value" in {
      val json = Json.obj("positionWithinBusiness" -> "")
      Json.fromJson[PositionWithinBusiness](json) must
        be(JsError((JsPath \ "positionWithinBusiness") -> ValidationError("error.invalid")))
    }

    "write the correct value for BeneficialOwner" in {
      Json.toJson(BeneficialOwner) must be(Json.obj("positionWithinBusiness" -> "01"))
    }

    "write the correct value for Director" in {
      Json.toJson(Director) must be(Json.obj("positionWithinBusiness" -> "02"))
    }

    "write the correct value for InternalAccountant" in {
      Json.toJson(InternalAccountant) must be(Json.obj("positionWithinBusiness" -> "03"))
    }

    "write the correct value for NominatedOfficer" in {
      Json.toJson(NominatedOfficer) must be(Json.obj("positionWithinBusiness" -> "04"))
    }

    "write the correct value for Partner" in {
      Json.toJson(Partner) must be(Json.obj("positionWithinBusiness" -> "05"))
    }

    "write the correct value for SoleProprietor" in {
      Json.toJson(SoleProprietor) must be(Json.obj("positionWithinBusiness" -> "06"))
    }
  }

}