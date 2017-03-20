package models.renewal

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class AMLSTurnoverSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "successfully validate given an enum value" in {

      AMLSTurnover.formRule.validate(Map("AMLSTurnover" -> Seq("01"))) must
        be(Valid(AMLSTurnover.First))

      AMLSTurnover.formRule.validate(Map("AMLSTurnover" -> Seq("02"))) must
        be(Valid(AMLSTurnover.Second))

      AMLSTurnover.formRule.validate(Map("AMLSTurnover" -> Seq("03"))) must
        be(Valid(AMLSTurnover.Third))

      AMLSTurnover.formRule.validate(Map("AMLSTurnover" -> Seq("04"))) must
        be(Valid(AMLSTurnover.Fourth))

      AMLSTurnover.formRule.validate(Map("AMLSTurnover" -> Seq("05"))) must
        be(Valid(AMLSTurnover.Fifth))

      AMLSTurnover.formRule.validate(Map("AMLSTurnover" -> Seq("06"))) must
        be(Valid(AMLSTurnover.Sixth))

      AMLSTurnover.formRule.validate(Map("AMLSTurnover" -> Seq("07"))) must
        be(Valid(AMLSTurnover.Seventh))
    }

    "write correct data from enum value" in {

      AMLSTurnover.formWrites.writes(AMLSTurnover.First) must
        be(Map("AMLSTurnover" -> Seq("01")))

      AMLSTurnover.formWrites.writes(AMLSTurnover.Second) must
        be(Map("AMLSTurnover" -> Seq("02")))

      AMLSTurnover.formWrites.writes(AMLSTurnover.Third) must
        be(Map("AMLSTurnover" -> Seq("03")))

      AMLSTurnover.formWrites.writes(AMLSTurnover.Fourth) must
        be(Map("AMLSTurnover" -> Seq("04")))

      AMLSTurnover.formWrites.writes(AMLSTurnover.Fifth) must
        be(Map("AMLSTurnover" -> Seq("05")))

      AMLSTurnover.formWrites.writes(AMLSTurnover.Sixth) must
        be(Map("AMLSTurnover" -> Seq("06")))

      AMLSTurnover.formWrites.writes(AMLSTurnover.Seventh) must
        be(Map("AMLSTurnover" -> Seq("07")))
    }


    "throw error on invalid data" in {
      AMLSTurnover.formRule.validate(Map("AMLSTurnover" -> Seq("20"))) must
        be(Invalid(Seq((Path \ "AMLSTurnover", Seq(ValidationError("error.invalid"))))))
    }

    "throw error on empty data" in {
      AMLSTurnover.formRule.validate(Map.empty) must
        be(Invalid(Seq((Path \ "AMLSTurnover", Seq(ValidationError("error.required.ba.turnover.from.mlr"))))))
    }
  }

  "JSON validation" must {

    "successfully validate given an enum value" in {

      Json.fromJson[AMLSTurnover](Json.obj("AMLSTurnover" -> "01")) must
        be(JsSuccess(AMLSTurnover.First, JsPath))

      Json.fromJson[AMLSTurnover](Json.obj("AMLSTurnover" -> "02")) must
        be(JsSuccess(AMLSTurnover.Second, JsPath))

      Json.fromJson[AMLSTurnover](Json.obj("AMLSTurnover" -> "03")) must
        be(JsSuccess(AMLSTurnover.Third, JsPath))

      Json.fromJson[AMLSTurnover](Json.obj("AMLSTurnover" -> "04")) must
        be(JsSuccess(AMLSTurnover.Fourth, JsPath))

      Json.fromJson[AMLSTurnover](Json.obj("AMLSTurnover" -> "05")) must
        be(JsSuccess(AMLSTurnover.Fifth, JsPath))

      Json.fromJson[AMLSTurnover](Json.obj("AMLSTurnover" -> "06")) must
        be(JsSuccess(AMLSTurnover.Sixth, JsPath))

      Json.fromJson[AMLSTurnover](Json.obj("AMLSTurnover" -> "07")) must
        be(JsSuccess(AMLSTurnover.Seventh, JsPath))
    }

    "write the correct value" in {

      Json.toJson(AMLSTurnover.First) must
        be(Json.obj("AMLSTurnover" -> "01"))

      Json.toJson(AMLSTurnover.Second) must
        be(Json.obj("AMLSTurnover" -> "02"))

      Json.toJson(AMLSTurnover.Third) must
        be(Json.obj("AMLSTurnover" -> "03"))

      Json.toJson(AMLSTurnover.Fourth) must
        be(Json.obj("AMLSTurnover" -> "04"))

      Json.toJson(AMLSTurnover.Fifth) must
        be(Json.obj("AMLSTurnover" -> "05"))

      Json.toJson(AMLSTurnover.Sixth) must
        be(Json.obj("AMLSTurnover" -> "06"))

      Json.toJson(AMLSTurnover.Seventh) must
        be(Json.obj("AMLSTurnover" -> "07"))
    }

    "throw error for invalid data" in {
      Json.fromJson[AMLSTurnover](Json.obj("AMLSTurnover" -> "20")) must
        be(JsError(JsPath, play.api.data.validation.ValidationError("error.invalid")))
    }
  }
}
