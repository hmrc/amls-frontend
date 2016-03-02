package models.businessactivities

import models.aboutyou.InternalAccountant
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class ExpectedAMLSTurnoverControllerSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "successfully validate given an enum value" in {

      ExpectedAMLSTurnover.formRule.validate(Map("expectedAMLSTurnover" -> Seq("01"))) must
        be(Success(ExpectedAMLSTurnover.First))

      ExpectedAMLSTurnover.formRule.validate(Map("expectedAMLSTurnover" -> Seq("02"))) must
        be(Success(ExpectedAMLSTurnover.Second))

      ExpectedAMLSTurnover.formRule.validate(Map("expectedAMLSTurnover" -> Seq("03"))) must
        be(Success(ExpectedAMLSTurnover.Third))

      ExpectedAMLSTurnover.formRule.validate(Map("expectedAMLSTurnover" -> Seq("04"))) must
        be(Success(ExpectedAMLSTurnover.Fourth))

      ExpectedAMLSTurnover.formRule.validate(Map("expectedAMLSTurnover" -> Seq("05"))) must
        be(Success(ExpectedAMLSTurnover.Fifth))

      ExpectedAMLSTurnover.formRule.validate(Map("expectedAMLSTurnover" -> Seq("06"))) must
        be(Success(ExpectedAMLSTurnover.Sixth))

      ExpectedAMLSTurnover.formRule.validate(Map("expectedAMLSTurnover" -> Seq("07"))) must
        be(Success(ExpectedAMLSTurnover.Seventh))
    }


    }

    "write correct data from enum value" in {

      ExpectedAMLSTurnover.formWrites.writes(ExpectedAMLSTurnover.First) must
        be(Map("expectedAMLSTurnover" -> Seq("01")))

      ExpectedAMLSTurnover.formWrites.writes(ExpectedAMLSTurnover.Second) must
        be(Map("expectedAMLSTurnover" -> Seq("02")))

      ExpectedAMLSTurnover.formWrites.writes(ExpectedAMLSTurnover.Third) must
        be(Map("expectedAMLSTurnover" -> Seq("03")))

      ExpectedAMLSTurnover.formWrites.writes(ExpectedAMLSTurnover.Fourth) must
        be(Map("expectedAMLSTurnover" -> Seq("04")))

      ExpectedAMLSTurnover.formWrites.writes(ExpectedAMLSTurnover.Fifth) must
        be(Map("expectedAMLSTurnover" -> Seq("05")))

      ExpectedAMLSTurnover.formWrites.writes(ExpectedAMLSTurnover.Sixth) must
        be(Map("expectedAMLSTurnover" -> Seq("06")))

      ExpectedAMLSTurnover.formWrites.writes(ExpectedAMLSTurnover.Seventh) must
        be(Map("expectedAMLSTurnover" -> Seq("07")))
    }

  "JSON validation" must {

    "successfully validate given an enum value" in {

      Json.fromJson[ExpectedAMLSTurnover](Json.obj("expectedAMLSTurnover" -> "01")) must
        be(JsSuccess(ExpectedAMLSTurnover.First, JsPath \ "expectedAMLSTurnover"))

      Json.fromJson[ExpectedAMLSTurnover](Json.obj("expectedAMLSTurnover" -> "02")) must
        be(JsSuccess(ExpectedAMLSTurnover.Second, JsPath \ "expectedAMLSTurnover"))

      Json.fromJson[ExpectedAMLSTurnover](Json.obj("expectedAMLSTurnover" -> "03")) must
        be(JsSuccess(ExpectedAMLSTurnover.Third, JsPath \ "expectedAMLSTurnover"))

      Json.fromJson[ExpectedAMLSTurnover](Json.obj("expectedAMLSTurnover" -> "04")) must
        be(JsSuccess(ExpectedAMLSTurnover.Fourth, JsPath \ "expectedAMLSTurnover"))

      Json.fromJson[ExpectedAMLSTurnover](Json.obj("expectedAMLSTurnover" -> "05")) must
        be(JsSuccess(ExpectedAMLSTurnover.Fifth, JsPath \ "expectedAMLSTurnover"))

      Json.fromJson[ExpectedAMLSTurnover](Json.obj("expectedAMLSTurnover" -> "06")) must
        be(JsSuccess(ExpectedAMLSTurnover.Sixth, JsPath \ "expectedAMLSTurnover"))

      Json.fromJson[ExpectedAMLSTurnover](Json.obj("expectedAMLSTurnover" -> "07")) must
        be(JsSuccess(ExpectedAMLSTurnover.Seventh, JsPath \ "expectedAMLSTurnover"))
    }


    "write the correct value" in {

      Json.toJson(ExpectedAMLSTurnover.First) must
        be(Json.obj("expectedAMLSTurnover" -> "01"))

      Json.toJson(ExpectedAMLSTurnover.Second) must
        be(Json.obj("expectedAMLSTurnover" -> "02"))

      Json.toJson(ExpectedAMLSTurnover.Third) must
        be(Json.obj("expectedAMLSTurnover" -> "03"))

      Json.toJson(ExpectedAMLSTurnover.Fourth) must
        be(Json.obj("expectedAMLSTurnover" -> "04"))

      Json.toJson(ExpectedAMLSTurnover.Fifth) must
        be(Json.obj("expectedAMLSTurnover" -> "05"))

      Json.toJson(ExpectedAMLSTurnover.Sixth) must
        be(Json.obj("expectedAMLSTurnover" -> "06"))

      Json.toJson(ExpectedAMLSTurnover.Seventh) must
        be(Json.obj("expectedAMLSTurnover" -> "07"))


    }
  }
}
