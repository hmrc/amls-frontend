package models.responsiblepeople

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class AddressHistorySpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "successfully validate given an enum value" in {

      AddressHistory.formRule.validate(Map("addressHistory" -> Seq("01"))) must
        be(Success(AddressHistory.First))

      AddressHistory.formRule.validate(Map("addressHistory" -> Seq("02"))) must
        be(Success(AddressHistory.Second))

      AddressHistory.formRule.validate(Map("addressHistory" -> Seq("03"))) must
        be(Success(AddressHistory.Third))

      AddressHistory.formRule.validate(Map("addressHistory" -> Seq("04"))) must
        be(Success(AddressHistory.Fourth))
    }

    "write correct data from enum value" in {

      AddressHistory.formWrites.writes(AddressHistory.First) must
        be(Map("addressHistory" -> Seq("01")))

      AddressHistory.formWrites.writes(AddressHistory.Second) must
        be(Map("addressHistory" -> Seq("02")))

      AddressHistory.formWrites.writes(AddressHistory.Third) must
        be(Map("addressHistory" -> Seq("03")))

      AddressHistory.formWrites.writes(AddressHistory.Fourth) must
        be(Map("addressHistory" -> Seq("04")))
    }


    "throw error on invalid data" in {
      AddressHistory.formRule.validate(Map("addressHistory" -> Seq("20"))) must
        be(Failure(Seq((Path \ "addressHistory", Seq(ValidationError("error.invalid"))))))
    }

    "throw error on empty data" in {
      AddressHistory.formRule.validate(Map.empty) must
        be(Failure(Seq((Path \ "addressHistory", Seq(ValidationError("error.required.rp.wherepersonlives.howlonglived"))))))
    }
  }

  "JSON validation" must {

    "successfully validate given an enum value" in {

      Json.fromJson[AddressHistory](Json.obj("addressHistory" -> "01")) must
        be(JsSuccess(AddressHistory.First, JsPath \ "addressHistory"))

      Json.fromJson[AddressHistory](Json.obj("addressHistory" -> "02")) must
        be(JsSuccess(AddressHistory.Second, JsPath \ "addressHistory"))

      Json.fromJson[AddressHistory](Json.obj("addressHistory" -> "03")) must
        be(JsSuccess(AddressHistory.Third, JsPath \ "addressHistory"))

      Json.fromJson[AddressHistory](Json.obj("addressHistory" -> "04")) must
        be(JsSuccess(AddressHistory.Fourth, JsPath \ "addressHistory"))
    }

    "write the correct value" in {

      Json.toJson(AddressHistory.First) must
        be(Json.obj("addressHistory" -> "01"))

      Json.toJson(AddressHistory.Second) must
        be(Json.obj("addressHistory" -> "02"))

      Json.toJson(AddressHistory.Third) must
        be(Json.obj("addressHistory" -> "03"))

      Json.toJson(AddressHistory.Fourth) must
        be(Json.obj("addressHistory" -> "04"))
    }

    "throw error for invalid data" in {
      Json.fromJson[AddressHistory](Json.obj("addressHistory" -> "20")) must
        be(JsError(JsPath \ "addressHistory", ValidationError("error.invalid")))
    }
  }
}
