package models.businessactivities

import models.estateagentbusiness.Services
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}


class TransactionTypeSpec extends PlaySpec with MockitoSugar {

  "TransactionType" must {

    import play.api.data.mapping.forms.Rules._

    "validate model with few check box selected" in {

      val model = Map(
        "isRecorded" -> Seq("true"),
        "transactions[]" -> Seq("01", "02" ,"03"),
        "name" -> Seq("test")
      )

      TransactionType.formRule.validate(model) must
        be(Success(TransactionRecordYes(Set(Paper, DigitalSpreadsheet, DigitalSoftware("test")))))

    }

    "fail validation when field is recorded not selected" in {

      val model = Map(
        "transactions[]" -> Seq("01", "02" ,"03"),
        "name" -> Seq("test")
      )

      TransactionType.formRule.validate(model) must
        be(Failure(List(( Path \ "isRecorded", Seq(ValidationError("error.required"))))))

    }

    "fail validation when none of the check boxes selected" in {

      val model = Map(
        "isRecorded" -> Seq("true"),
        "transactions[]" -> Seq(),
        "name" -> Seq("test")
      )

      TransactionType.formRule.validate(model) must
        be(Failure(List(( Path \ "transactions", Seq(ValidationError("error.required"))))))

    }


    "fail to validate on empty Map" in {

      TransactionType.formRule.validate(Map.empty) must
        be(Failure(Seq((Path \ "isRecorded") -> Seq(ValidationError("error.required")))))

    }

    "validate form write for valid transaction record" in {

      val map = Map(
        "isRecorded" -> Seq("true"),
        "transactions" -> Seq("03","01"),
        "name" -> Seq("test")
      )

      val model = TransactionRecordYes(Set(DigitalSoftware("test"), Paper))
     TransactionType.formWrites.writes(model) must be (map)
    }

    "validate form write for option No" in {

      val map = Map(
        "isRecorded" -> Seq("false")
      )

      val model = TransactionRecordNo
      TransactionType.formWrites.writes(model) must be (map)
    }

    "validate form write for option Yes" in {

      val map = Map(
        "isRecorded" -> Seq("true"),
        "transactions" -> Seq("02","01")
      )

      val model = TransactionRecordYes(Set(DigitalSpreadsheet, Paper))
      TransactionType.formWrites.writes(model) must be (map)
    }


    "form write test" in {
      val map = Map(
        "isRecorded" -> Seq("false")
      )

      val model = TransactionRecordNo

      TransactionType.formWrites.writes(model) must be(map)
    }

    "JSON validation" must {

      "successfully validate given values" in {
        val json =  Json.obj("isRecorded" -> true,
          "transactions" -> Seq("01","02"))

        Json.fromJson[TransactionRecord](json) must
          be(JsSuccess(TransactionRecordYes(Set(Paper, DigitalSpreadsheet)), JsPath \ "isRecorded" \ "transactions"))
      }

      "successfully validate given values with option Digital software" in {
        val json =  Json.obj("isRecorded" -> true,
          "transactions" -> Seq("03", "02"),
        "name" -> "test")

        Json.fromJson[TransactionRecord](json) must
          be(JsSuccess(TransactionRecordYes(Set(DigitalSoftware("test"), DigitalSpreadsheet)), JsPath \ "isRecorded" \ "transactions" \ "name"))
      }

      "fail when on path is missing" in {
        Json.fromJson[TransactionRecord](Json.obj("isRecorded" -> true,
          "transaction" -> Seq("01"))) must
          be(JsError((JsPath \ "isRecorded" \ "transactions") -> ValidationError("error.path.missing")))
      }

     /* "fail when on invalid data" in {
        Json.fromJson[TransactionRecord](Json.obj("isRecorded" -> true,"transactions" -> Seq("40"))) must
          be(JsError(((JsPath \ "isRecorded" \ "transactions")(0) \ "transactions") -> ValidationError("error.invalid")))
      }*/
    }
  }
}
