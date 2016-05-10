package models.asp

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class OtherBusinessTaxMattersSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "successfully validate given enum value" in {
      OtherBusinessTaxMatters.formRule.validate(Map("otherBusinessTaxMatters" -> Seq("false"))) must
        be(Success(OtherBusinessTaxMattersNo))
    }

    "successfully validate given an `Yes` value" in {

      val data = Map(
        "otherBusinessTaxMatters" -> Seq("true"),
        "agentRegNo" -> Seq("12345678")
      )

      OtherBusinessTaxMatters.formRule.validate(data) must
        be(Success(OtherBusinessTaxMattersYes("12345678")))
    }

    "fail when mandatory fields are missing" in {

      OtherBusinessTaxMatters.formRule.validate(Map.empty) must
        be(Failure(Seq(
          (Path \ "otherBusinessTaxMatters") -> Seq(ValidationError("error.required.asp.other.business.tax.matters"))
        )))

    }

    "fail to validate given an `Yes` with no value" in {

      val data = Map(
        "otherBusinessTaxMatters" -> Seq("true"),
        "agentRegNo" -> Seq("")
      )

      OtherBusinessTaxMatters.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "agentRegNo") -> Seq(ValidationError("error.required.asp.agentRegNo"))
        )))
    }

    "fail to validate given an `Yes` with more than max length value" in {

      val data = Map(
        "otherBusinessTaxMatters" -> Seq("true"),
        "agentRegNo" -> Seq("123qed1258963")
      )

      OtherBusinessTaxMatters.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "agentRegNo") -> Seq(ValidationError("error.invalid.length.asp.agentRegNo"))
        )))
    }

    "fail to validate given an `Yes` with invalid value" in {

      val data = Map(
        "otherBusinessTaxMatters" -> Seq("true"),
        "agentRegNo" -> Seq("123qe")
      )

      OtherBusinessTaxMatters.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "agentRegNo") -> Seq(ValidationError("error.invalid.asp.agentRegNo"))
        )))
    }

    "write correct data from enum value" in {

      OtherBusinessTaxMatters.formWrites.writes(OtherBusinessTaxMattersNo) must
        be(Map("otherBusinessTaxMatters" -> Seq("false")))

    }

    "write correct data from `yes` value" in {

      OtherBusinessTaxMatters.formWrites.writes(OtherBusinessTaxMattersYes("12345678")) must
        be(Map("otherBusinessTaxMatters" -> Seq("true"), "agentRegNo" -> Seq("12345678")))

    }

  }

  "Json validation" must {

    "successfully validate given an enum value" in {

      Json.fromJson[OtherBusinessTaxMatters](Json.obj("otherBusinessTaxMatters" -> false)) must
        be(JsSuccess(OtherBusinessTaxMattersNo, JsPath \ "otherBusinessTaxMatters"))
    }

    "successfully validate given an `Yes` value" in {

      Json.fromJson[OtherBusinessTaxMatters](Json.obj("otherBusinessTaxMatters" -> true, "agentRegNo" -> "12345678")) must
        be(JsSuccess(OtherBusinessTaxMattersYes("12345678"), JsPath \ "otherBusinessTaxMatters" \ "agentRegNo"))
    }

    "fail to validate when given an empty `Yes` value" in {

      val json = Json.obj("otherBusinessTaxMatters" -> true)

      Json.fromJson[OtherBusinessTaxMatters](json) must
        be(JsError((JsPath \ "otherBusinessTaxMatters" \ "agentRegNo") -> ValidationError("error.path.missing")))
    }

    "write the correct value" in {

      Json.toJson(OtherBusinessTaxMattersNo) must
        be(Json.obj("otherBusinessTaxMatters" -> false))

      Json.toJson(OtherBusinessTaxMattersYes("12345678")) must
        be(Json.obj(
          "otherBusinessTaxMatters" -> true,
          "agentRegNo" -> "12345678"
        ))
    }
  }

}
