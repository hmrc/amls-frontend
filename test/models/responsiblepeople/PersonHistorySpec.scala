package models.responsiblepeople

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class PersonHistorySpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "successfully validate given an enum value" in {

      PersonHistory.formRule.validate(Map("personHistory" -> Seq("01"))) must
        be(Success(PersonHistory.First))

      PersonHistory.formRule.validate(Map("personHistory" -> Seq("02"))) must
        be(Success(PersonHistory.Second))

      PersonHistory.formRule.validate(Map("personHistory" -> Seq("03"))) must
        be(Success(PersonHistory.Third))

      PersonHistory.formRule.validate(Map("personHistory" -> Seq("04"))) must
        be(Success(PersonHistory.Fourth))
    }

    "write correct data from enum value" in {

      PersonHistory.formWrites.writes(PersonHistory.First) must
        be(Map("personHistory" -> Seq("01")))

      PersonHistory.formWrites.writes(PersonHistory.Second) must
        be(Map("personHistory" -> Seq("02")))

      PersonHistory.formWrites.writes(PersonHistory.Third) must
        be(Map("personHistory" -> Seq("03")))

      PersonHistory.formWrites.writes(PersonHistory.Fourth) must
        be(Map("personHistory" -> Seq("04")))
    }


    "throw error on invalid data" in {
      PersonHistory.formRule.validate(Map("personHistory" -> Seq("20"))) must
        be(Failure(Seq((Path \ "personHistory", Seq(ValidationError("error.invalid"))))))
    }

    "throw error on empty data" in {
      PersonHistory.formRule.validate(Map.empty) must
        be(Failure(Seq((Path \ "personHistory", Seq(ValidationError("error.required.ba.turnover.from.mlr"))))))
    }
  }

  "JSON validation" must {

    "successfully validate given an enum value" in {

      Json.fromJson[PersonHistory](Json.obj("personHistory" -> "01")) must
        be(JsSuccess(PersonHistory.First, JsPath \ "personHistory"))

      Json.fromJson[PersonHistory](Json.obj("personHistory" -> "02")) must
        be(JsSuccess(PersonHistory.Second, JsPath \ "personHistory"))

      Json.fromJson[PersonHistory](Json.obj("personHistory" -> "03")) must
        be(JsSuccess(PersonHistory.Third, JsPath \ "personHistory"))

      Json.fromJson[PersonHistory](Json.obj("personHistory" -> "04")) must
        be(JsSuccess(PersonHistory.Fourth, JsPath \ "personHistory"))
    }

    "write the correct value" in {

      Json.toJson(PersonHistory.First) must
        be(Json.obj("personHistory" -> "01"))

      Json.toJson(PersonHistory.Second) must
        be(Json.obj("personHistory" -> "02"))

      Json.toJson(PersonHistory.Third) must
        be(Json.obj("personHistory" -> "03"))

      Json.toJson(PersonHistory.Fourth) must
        be(Json.obj("personHistory" -> "04"))
    }

    "throw error for invalid data" in {
      Json.fromJson[PersonHistory](Json.obj("personHistory" -> "20")) must
        be(JsError(JsPath \ "personHistory", ValidationError("error.invalid")))
    }
  }
}
