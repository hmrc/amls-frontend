package models.businessactivities

import models.aboutyou.InternalAccountant
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class ExpectedBusinessTurnoverSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "successfully validate given an enum value" in {
      ExpectedBusinessTurnover.formRule.validate(Map("expectedBusinessTurnover" -> Seq("01"))) must
        be(Success(ExpectedBusinessTurnover.First))

      ExpectedBusinessTurnover.formRule.validate(Map("expectedBusinessTurnover" -> Seq("02"))) must
        be(Success(ExpectedBusinessTurnover.Second))

      ExpectedBusinessTurnover.formRule.validate(Map("expectedBusinessTurnover" -> Seq("03"))) must
        be(Success(ExpectedBusinessTurnover.Third))

      ExpectedBusinessTurnover.formRule.validate(Map("expectedBusinessTurnover" -> Seq("04"))) must
        be(Success(ExpectedBusinessTurnover.Fourth))

      ExpectedBusinessTurnover.formRule.validate(Map("expectedBusinessTurnover" -> Seq("05"))) must
        be(Success(ExpectedBusinessTurnover.Fifth))

      ExpectedBusinessTurnover.formRule.validate(Map("expectedBusinessTurnover" -> Seq("06"))) must
        be(Success(ExpectedBusinessTurnover.Sixth))

      ExpectedBusinessTurnover.formRule.validate(Map("expectedBusinessTurnover" -> Seq("07"))) must
        be(Success(ExpectedBusinessTurnover.Seventh))
    }

    "throw error on invalid data" in {
      ExpectedBusinessTurnover.formRule.validate(Map("expectedBusinessTurnover" -> Seq("20"))) must
        be(Failure(Seq((Path \ "expectedBusinessTurnover", Seq(ValidationError("error.invalid"))))))
    }

    "write correct data from enum value" in {

      ExpectedBusinessTurnover.formWrites.writes(ExpectedBusinessTurnover.First) must
        be(Map("expectedBusinessTurnover" -> Seq("01")))

      ExpectedBusinessTurnover.formWrites.writes(ExpectedBusinessTurnover.Second) must
        be(Map("expectedBusinessTurnover" -> Seq("02")))

      ExpectedBusinessTurnover.formWrites.writes(ExpectedBusinessTurnover.Third) must
        be(Map("expectedBusinessTurnover" -> Seq("03")))

      ExpectedBusinessTurnover.formWrites.writes(ExpectedBusinessTurnover.Fourth) must
        be(Map("expectedBusinessTurnover" -> Seq("04")))

      ExpectedBusinessTurnover.formWrites.writes(ExpectedBusinessTurnover.Fifth) must
        be(Map("expectedBusinessTurnover" -> Seq("05")))

      ExpectedBusinessTurnover.formWrites.writes(ExpectedBusinessTurnover.Sixth) must
        be(Map("expectedBusinessTurnover" -> Seq("06")))

      ExpectedBusinessTurnover.formWrites.writes(ExpectedBusinessTurnover.Seventh) must
        be(Map("expectedBusinessTurnover" -> Seq("07")))
    }
  }

  "JSON validation" must {

    "successfully validate given an enum value" in {

      Json.fromJson[ExpectedBusinessTurnover](Json.obj("expectedBusinessTurnover" -> "01")) must
        be(JsSuccess(ExpectedBusinessTurnover.First, JsPath \ "expectedBusinessTurnover"))

      Json.fromJson[ExpectedBusinessTurnover](Json.obj("expectedBusinessTurnover" -> "02")) must
        be(JsSuccess(ExpectedBusinessTurnover.Second, JsPath \ "expectedBusinessTurnover"))

      Json.fromJson[ExpectedBusinessTurnover](Json.obj("expectedBusinessTurnover" -> "03")) must
        be(JsSuccess(ExpectedBusinessTurnover.Third, JsPath \ "expectedBusinessTurnover"))

      Json.fromJson[ExpectedBusinessTurnover](Json.obj("expectedBusinessTurnover" -> "04")) must
        be(JsSuccess(ExpectedBusinessTurnover.Fourth, JsPath \ "expectedBusinessTurnover"))

      Json.fromJson[ExpectedBusinessTurnover](Json.obj("expectedBusinessTurnover" -> "05")) must
        be(JsSuccess(ExpectedBusinessTurnover.Fifth, JsPath \ "expectedBusinessTurnover"))

      Json.fromJson[ExpectedBusinessTurnover](Json.obj("expectedBusinessTurnover" -> "06")) must
        be(JsSuccess(ExpectedBusinessTurnover.Sixth, JsPath \ "expectedBusinessTurnover"))

      Json.fromJson[ExpectedBusinessTurnover](Json.obj("expectedBusinessTurnover" -> "07")) must
        be(JsSuccess(ExpectedBusinessTurnover.Seventh, JsPath \ "expectedBusinessTurnover"))
    }


    "write the correct value" in {
      Json.toJson(ExpectedBusinessTurnover.First) must
        be(Json.obj("expectedBusinessTurnover" -> "01"))

      Json.toJson(ExpectedBusinessTurnover.Second) must
        be(Json.obj("expectedBusinessTurnover" -> "02"))

      Json.toJson(ExpectedBusinessTurnover.Third) must
        be(Json.obj("expectedBusinessTurnover" -> "03"))

      Json.toJson(ExpectedBusinessTurnover.Fourth) must
        be(Json.obj("expectedBusinessTurnover" -> "04"))

      Json.toJson(ExpectedBusinessTurnover.Fifth) must
        be(Json.obj("expectedBusinessTurnover" -> "05"))

      Json.toJson(ExpectedBusinessTurnover.Sixth) must
        be(Json.obj("expectedBusinessTurnover" -> "06"))

      Json.toJson(ExpectedBusinessTurnover.Seventh) must
        be(Json.obj("expectedBusinessTurnover" -> "07"))
    }

    "throw error for invalid data" in {
      Json.fromJson[ExpectedBusinessTurnover](Json.obj("expectedBusinessTurnover" -> "20")) must
        be(JsError(JsPath \ "expectedBusinessTurnover", ValidationError("error.invalid")))
    }
  }
}
