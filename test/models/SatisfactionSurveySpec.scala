package models

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Failure, Path, Success}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}
import SatisfactionSurvey._
class SatisfactionSurveySpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {


    "successfully validate given feedback with empty details" in {
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("01"), "details" -> Seq(""))) must
        be(Success(First(None)))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("02"), "details" -> Seq(""))) must
        be(Success(Second(None)))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("03"), "details" -> Seq(""))) must
        be(Success(Third(None)))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("04"), "details" -> Seq(""))) must
        be(Success(Fourth(None)))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("05"), "details" -> Seq(""))) must
        be(Success(Fifth(None)))
    }

    "successfully validate given feedback with details" in {
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("01"), "details" -> Seq("123"))) must
        be(Success(First(Some("123"))))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("02"), "details" -> Seq("123"))) must
        be(Success(Second(Some("123"))))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("03"), "details" -> Seq("123"))) must
        be(Success(Third(Some("123"))))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("04"), "details" -> Seq("123"))) must
        be(Success(Fourth(Some("123"))))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("05"), "details" -> Seq("123"))) must
        be(Success(Fifth(Some("123"))))
    }

    "successfully validate missing mandatory details" in {
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("01"))) must
        be(Success(First(None)))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("02"))) must
        be(Success(Second(None)))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("03"))) must
        be(Success(Third(None)))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("04"))) must
        be(Success(Fourth(None)))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("05"))) must
        be(Success(Fifth(None)))
    }

    "fail to validate missing mandatory satisfaction" in {
      val data = Map(
        "details" -> Seq("")
      )
      SatisfactionSurvey.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "satisfaction") -> Seq(ValidationError("error.survey.satisfaction.required"))
        )))
    }

    "fail to validate empty data" in {
      SatisfactionSurvey.formRule.validate(Map.empty) must
        be(Failure(Seq(
          (Path \ "satisfaction") -> Seq(ValidationError("error.survey.satisfaction.required"))
        )))
    }

    "fail to validate details over max value" in {
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("01"),"details" -> Seq("zzxczxczx"*150))) must
        be(Failure(Seq(
          (Path \ "details") -> Seq(ValidationError("error.invalid.maxlength.1200"))
        )))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("02"),"details" -> Seq("zzxczxczx"*150))) must
        be(Failure(Seq(
          (Path \ "details") -> Seq(ValidationError("error.invalid.maxlength.1200"))
        )))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("03"),"details" -> Seq("zzxczxczx"*150))) must
        be(Failure(Seq(
          (Path \ "details") -> Seq(ValidationError("error.invalid.maxlength.1200"))
        )))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("04"),"details" -> Seq("zzxczxczx"*150))) must
        be(Failure(Seq(
          (Path \ "details") -> Seq(ValidationError("error.invalid.maxlength.1200"))
        )))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("05"),"details" -> Seq("zzxczxczx"*150))) must
        be(Failure(Seq(
          (Path \ "details") -> Seq(ValidationError("error.invalid.maxlength.1200"))
        )))
    }

  }

  "JSON validation" must {

    "successfully validate given feedback with empty details" in {
      Json.fromJson[SatisfactionSurvey](Json.obj("satisfaction" -> "01")) must
        be(JsSuccess(First(None), JsPath \ "satisfaction"))
      Json.fromJson[SatisfactionSurvey](Json.obj("satisfaction" -> "02")) must
        be(JsSuccess(Second(None), JsPath \ "satisfaction"))
      Json.fromJson[SatisfactionSurvey](Json.obj("satisfaction" -> "03")) must
        be(JsSuccess(Third(None), JsPath \ "satisfaction"))
      Json.fromJson[SatisfactionSurvey](Json.obj("satisfaction" -> "04")) must
        be(JsSuccess(Fourth(None), JsPath \ "satisfaction"))
      Json.fromJson[SatisfactionSurvey](Json.obj("satisfaction" -> "05")) must
        be(JsSuccess(Fifth(None), JsPath \ "satisfaction"))
    }

    "successfully validate given feedback with details" in {
      Json.fromJson[SatisfactionSurvey](Json.obj("satisfaction" -> "01", "details" ->"123")) must
        be(JsSuccess(First(Some("123")), JsPath \ "satisfaction" \ "details"))
      Json.fromJson[SatisfactionSurvey](Json.obj("satisfaction" -> "02", "details" ->"123")) must
        be(JsSuccess(Second(Some("123")), JsPath \ "satisfaction" \ "details"))
      Json.fromJson[SatisfactionSurvey](Json.obj("satisfaction" -> "03", "details" ->"123")) must
        be(JsSuccess(Third(Some("123")), JsPath \ "satisfaction" \ "details"))
      Json.fromJson[SatisfactionSurvey](Json.obj("satisfaction" -> "04", "details" ->"123")) must
        be(JsSuccess(Fourth(Some("123")), JsPath \ "satisfaction" \ "details"))
      Json.fromJson[SatisfactionSurvey](Json.obj("satisfaction" -> "05", "details" ->"123")) must
        be(JsSuccess(Fifth(Some("123")), JsPath \ "satisfaction" \ "details"))

    }

    "fail to validate given no data" in {
      val json = Json.obj()
      Json.fromJson[SatisfactionSurvey](json) must
        be(JsError((JsPath \ "satisfaction") -> ValidationError("error.path.missing")))
    }

  }

  "write the correct value" in {
    Json.toJson(First(None)) must
      be(Json.obj("satisfaction" -> "01", "details" -> ""))
    Json.toJson(First(Some("123"))) must
      be(Json.obj(
        "satisfaction" -> "01",
        "details" -> "123"
      ))

    Json.toJson(Second(None)) must
      be(Json.obj("satisfaction" -> "02", "details" -> ""))
    Json.toJson(Second(Some("123"))) must
      be(Json.obj(
        "satisfaction" -> "02",
        "details" -> "123"
      ))

    Json.toJson(Third(None)) must
      be(Json.obj("satisfaction" -> "03", "details" -> ""))
    Json.toJson(Third(Some("123"))) must
      be(Json.obj(
        "satisfaction" -> "03",
        "details" -> "123"
      ))

    Json.toJson(Fourth(None)) must
      be(Json.obj("satisfaction" -> "04", "details" -> ""))
    Json.toJson(Fourth(Some("123"))) must
      be(Json.obj(
        "satisfaction" -> "04",
        "details" -> "123"
      ))

    Json.toJson(Fifth(None)) must
      be(Json.obj("satisfaction" -> "05", "details" -> ""))
    Json.toJson(Fifth(Some("123"))) must
      be(Json.obj(
        "satisfaction" -> "05",
        "details" -> "123"
      ))
  }


}
