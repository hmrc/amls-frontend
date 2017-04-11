package models.renewal

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class AMLSTurnoverSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "successfully validate given an enum value" in {

      AMLSTurnover.formRule.validate(Map("turnover" -> Seq("01"))) must
        be(Valid(AMLSTurnover.First))

      AMLSTurnover.formRule.validate(Map("turnover" -> Seq("02"))) must
        be(Valid(AMLSTurnover.Second))

      AMLSTurnover.formRule.validate(Map("turnover" -> Seq("03"))) must
        be(Valid(AMLSTurnover.Third))

      AMLSTurnover.formRule.validate(Map("turnover" -> Seq("04"))) must
        be(Valid(AMLSTurnover.Fourth))

      AMLSTurnover.formRule.validate(Map("turnover" -> Seq("05"))) must
        be(Valid(AMLSTurnover.Fifth))

      AMLSTurnover.formRule.validate(Map("turnover" -> Seq("06"))) must
        be(Valid(AMLSTurnover.Sixth))

      AMLSTurnover.formRule.validate(Map("turnover" -> Seq("07"))) must
        be(Valid(AMLSTurnover.Seventh))
    }

    "write correct data from enum value" in {

      AMLSTurnover.formWrites.writes(AMLSTurnover.First) must
        be(Map("turnover" -> Seq("01")))

      AMLSTurnover.formWrites.writes(AMLSTurnover.Second) must
        be(Map("turnover" -> Seq("02")))

      AMLSTurnover.formWrites.writes(AMLSTurnover.Third) must
        be(Map("turnover" -> Seq("03")))

      AMLSTurnover.formWrites.writes(AMLSTurnover.Fourth) must
        be(Map("turnover" -> Seq("04")))

      AMLSTurnover.formWrites.writes(AMLSTurnover.Fifth) must
        be(Map("turnover" -> Seq("05")))

      AMLSTurnover.formWrites.writes(AMLSTurnover.Sixth) must
        be(Map("turnover" -> Seq("06")))

      AMLSTurnover.formWrites.writes(AMLSTurnover.Seventh) must
        be(Map("turnover" -> Seq("07")))
    }


    "throw error on invalid data" in {
      AMLSTurnover.formRule.validate(Map("turnover" -> Seq("20"))) must
        be(Invalid(Seq((Path \ "turnover", Seq(ValidationError("error.invalid"))))))
    }

    "throw error on empty data" in {
      AMLSTurnover.formRule.validate(Map.empty) must
        be(Invalid(Seq((Path \ "turnover", Seq(ValidationError("error.required.renewal.ba.turnover.from.mlr"))))))
    }
  }

  "JSON validation" must {

    "successfully validate given an enum value" in {

      Json.fromJson[AMLSTurnover](Json.obj("turnover" -> "01")) must
        be(JsSuccess(AMLSTurnover.First, JsPath))

      Json.fromJson[AMLSTurnover](Json.obj("turnover" -> "02")) must
        be(JsSuccess(AMLSTurnover.Second, JsPath))

      Json.fromJson[AMLSTurnover](Json.obj("turnover" -> "03")) must
        be(JsSuccess(AMLSTurnover.Third, JsPath))

      Json.fromJson[AMLSTurnover](Json.obj("turnover" -> "04")) must
        be(JsSuccess(AMLSTurnover.Fourth, JsPath))

      Json.fromJson[AMLSTurnover](Json.obj("turnover" -> "05")) must
        be(JsSuccess(AMLSTurnover.Fifth, JsPath))

      Json.fromJson[AMLSTurnover](Json.obj("turnover" -> "06")) must
        be(JsSuccess(AMLSTurnover.Sixth, JsPath))

      Json.fromJson[AMLSTurnover](Json.obj("turnover" -> "07")) must
        be(JsSuccess(AMLSTurnover.Seventh, JsPath))
    }

    "write the correct value" in {

      Json.toJson(AMLSTurnover.First) must
        be(Json.obj("turnover" -> "01"))

      Json.toJson(AMLSTurnover.Second) must
        be(Json.obj("turnover" -> "02"))

      Json.toJson(AMLSTurnover.Third) must
        be(Json.obj("turnover" -> "03"))

      Json.toJson(AMLSTurnover.Fourth) must
        be(Json.obj("turnover" -> "04"))

      Json.toJson(AMLSTurnover.Fifth) must
        be(Json.obj("turnover" -> "05"))

      Json.toJson(AMLSTurnover.Sixth) must
        be(Json.obj("turnover" -> "06"))

      Json.toJson(AMLSTurnover.Seventh) must
        be(Json.obj("turnover" -> "07"))
    }

    "throw error for invalid data" in {
      Json.fromJson[AMLSTurnover](Json.obj("turnover" -> "20")) must
        be(JsError(JsPath, play.api.data.validation.ValidationError("error.invalid")))
    }
  }
}
