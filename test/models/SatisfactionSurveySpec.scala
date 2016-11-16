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
        be(Success(First(None)))
    }

    "successfully validate given feedback with details" in {

      val data = Map(
        "satisfaction" -> Seq("02"),
        "details" -> Seq("123")
      )

      SatisfactionSurvey.formRule.validate(data) must
        be(Success(Second(Some("123"))))
    }

    "successfully validate missing mandatory details" in {

      val data = Map(
        "satisfaction" -> Seq("02")
      )

      SatisfactionSurvey.formRule.validate(data) must
        be(Success(Second(None)))

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

  }

  "JSON validation" must {

    "successfully validate given feedback with empty details" in {

      Json.fromJson[SatisfactionSurvey](Json.obj("satisfaction" -> "04")) must
        be(JsSuccess(Fourth(None), JsPath \ "satisfaction"))
    }

    "successfully validate given feedback with details" in {

      val json = Json.obj("satisfaction" -> "03", "details" ->"123")

      Json.fromJson[SatisfactionSurvey](json) must
        be(JsSuccess(Third(Some("123")), JsPath \ "satisfaction" \ "details"))
    }

    "fail to validate given no data" in {

      val json = Json.obj()

      Json.fromJson[SatisfactionSurvey](json) must
        be(JsError((JsPath \ "satisfaction") -> ValidationError("error.path.missing")))
    }

  }


}
