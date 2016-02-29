package models.businessactivities

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class BusinessFranchiseSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {
    "successfully validate given an enum value" in {
      BusinessFranchise.formRule.validate(Map("businessFranchise" -> Seq("false"))) must
        be(Success(BusinessFranchiseNo))
    }

    "successfully validate given an `Yes` value" in {
      val data = Map(
        "businessFranchise" -> Seq("true"),
        "franchiseName" -> Seq("test test")
      )

      BusinessFranchise.formRule.validate(data) must
        be(Success(BusinessFranchiseYes("test test")))
    }

    "fail to validate given an `Yes` with no value" in {

      val data = Map(
        "businessFranchise" -> Seq("true")
      )

      BusinessFranchise.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "franchiseName") -> Seq(ValidationError("error.required"))
        )))
    }

    "write correct data from enum value" in {

      BusinessFranchise.formWrites.writes(BusinessFranchiseNo) must
        be(Map("businessFranchise" -> Seq("false")))

    }

    "write correct data from `Yes` value" in {

      BusinessFranchise.formWrites.writes(BusinessFranchiseYes("test test")) must
        be(Map("businessFranchise" -> Seq("true"), "franchiseName" -> Seq("test test")))
    }

  }

  "JSON validation" must {
    "successfully validate given an enum value" in {

      Json.fromJson[BusinessFranchise](Json.obj("businessFranchise" -> false)) must
        be(JsSuccess(BusinessFranchiseNo, JsPath \ "businessFranchise"))
    }

    "successfully validate given an `Yes` value" in {

      val json = Json.obj("businessFranchise" -> true, "franchiseName" ->"test test")

      Json.fromJson[BusinessFranchise](json) must
        be(JsSuccess(BusinessFranchiseYes("test test"), JsPath \ "businessFranchise" \ "franchiseName"))
    }

    "fail to validate when given an empty `Yes` value" in {

      val json = Json.obj("businessFranchise" -> true)

      Json.fromJson[BusinessFranchise](json) must
        be(JsError((JsPath \ "businessFranchise" \ "franchiseName") -> ValidationError("error.path.missing")))
    }

    "write the correct value" in {

      Json.toJson(BusinessFranchiseNo) must
        be(Json.obj("businessFranchise" -> false))

      Json.toJson(BusinessFranchiseYes("test test")) must
        be(Json.obj(
          "businessFranchise" -> true,
          "franchiseName" -> "test test"
        ))
    }
  }

}
