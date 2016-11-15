package models

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}
import SatisfactionSurvey._
class SatisfactionSurveySpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {


    "successfully validate given feedback with empty details" in {

      val data = Map(
        "satisfaction" -> Seq("01"),
        "details" -> Seq("")
      )

      SatisfactionSurvey.formRule.validate(data) must
        be(Success(First("")))
    }

    "successfully validate given feedback with details" in {

      val data = Map(
        "satisfaction" -> Seq("02"),
        "details" -> Seq("123")
      )

      SatisfactionSurvey.formRule.validate(data) must
        be(Success(Second("123")))
    }

    "fail to validate missing mandatory satisfaction" in {

      val data = Map(
        "details" -> Seq("")
      )

      SatisfactionSurvey.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "satisfaction") -> Seq(ValidationError("error.invalid"))
        )))
    }

    "fail to validate missing mandatory details" in {

      val data = Map(
        "satisfaction" -> Seq("02")
      )

      SatisfactionSurvey.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "details") -> Seq(ValidationError("error.required"))
        )))
    }

    "fail to validate empty data" in {

      SatisfactionSurvey.formRule.validate(Map.empty) must
        be(Failure(Seq(
          (Path \ "satisfaction") -> Seq(ValidationError("error.invalid"))
        )))
    }

    "fail to validate details over max value" in {

      val data = Map(
        "satisfaction" -> Seq("02"),
        "details" -> Seq("zzxczxczx"*50)
      )

      SatisfactionSurvey.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "details") -> Seq(ValidationError("error.invalid"))
        )))
    }

    "write correct data for feedback with empty details" in {

      SatisfactionSurvey.formWrites.writes(Third("")) must
        be(Map("satisfaction" -> Seq("03"), "details" -> Seq("")))

    }

    "write correct data for feedback with details" in {

      SatisfactionSurvey.formWrites.writes(Second("123")) must
        be(Map("satisfaction" -> Seq("02"), "details" -> Seq("123")))
    }
  }

  "JSON validation" must {

    "successfully validate given feedback with empty details" in {

      Json.fromJson[SatisfactionSurvey](Json.obj("satisfaction" -> "04", "details" -> "")) must
        be(JsSuccess(Fourth(""), JsPath \ "satisfaction" \ "details"))
    }

    "successfully validate given feedback with details" in {

      val json = Json.obj("satisfaction" -> "03", "details" ->"123")

      Json.fromJson[SatisfactionSurvey](json) must
        be(JsSuccess(Third("123"), JsPath \ "satisfaction" \ "details"))
    }

    "fail to validate given no details" in {

      val json = Json.obj("satisfaction" -> "04")

      Json.fromJson[SatisfactionSurvey](json) must
        be(JsError((JsPath \ "satisfaction" \ "details") -> ValidationError("error.path.missing")))
    }

    "write the correct value" in {

      Json.toJson(Fifth("")) must
        be(Json.obj("satisfaction" -> "05", "details" -> ""))

      Json.toJson(Fifth("123")) must
        be(Json.obj(
          "satisfaction" -> "05",
          "details" -> "123"
        ))
    }
  }


}
