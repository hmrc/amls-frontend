package models.businessactivities

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Path, Failure, Success}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}


class TransactionRecordSpec extends PlaySpec with MockitoSugar {

  "TransactionType" must {

    import jto.validation.forms.Rules._

    "validate model with few check box selected" in {

      val model = Map(
        "isRecorded" -> Seq("true"),
        "transactions[]" -> Seq("01", "02" ,"03"),
        "name" -> Seq("test")
      )

      TransactionRecord.formRule.validate(model) must
        be(Success(TransactionRecordYes(Set(Paper, DigitalSpreadsheet, DigitalSoftware("test")))))

    }

    "validate model with option No selected" in {

      val model = Map(
        "isRecorded" -> Seq("false")
      )

      TransactionRecord.formRule.validate(model) must
        be(Success(TransactionRecordNo))

    }

    "fail validation when field is recorded not selected" in {

      val model = Map(
        "transactions[]" -> Seq("01", "02" ,"03"),
        "name" -> Seq("")
      )

      TransactionRecord.formRule.validate(model) must
        be(Failure(List(( Path \ "isRecorded", Seq(ValidationError("error.required.ba.select.transaction.record"))))))

    }

    "fail validation when field is recorded selected and software name is empty" in {

      val model = Map(
        "isRecorded" -> Seq("true"),
        "transactions[]" -> Seq("01", "02" ,"03"),
        "name" -> Seq("")
      )
      TransactionRecord.formRule.validate(model) must
        be(Failure(List(( Path \ "name", Seq(ValidationError("error.required.ba.software.package.name"))))))
    }

    "fail validation when field is recorded selected and software name exceed max length" in {

      val model = Map(
        "isRecorded" -> Seq("true"),
        "transactions[]" -> Seq("01", "02" ,"03"),
        "name" -> Seq("test"*20)
      )
      TransactionRecord.formRule.validate(model) must
        be(Failure(List(( Path \ "name", Seq(ValidationError("error.max.length.ba.software.package.name"))))))
    }

    "fail validation when none of the check boxes selected" in {

      val model = Map(
        "isRecorded" -> Seq("true"),
        "transactions[]" -> Seq(),
        "name" -> Seq("test")
      )
      TransactionRecord.formRule.validate(model) must
        be(Failure(List(( Path \ "transactions", Seq(ValidationError("error.required.ba.atleast.one.transaction.record"))))))
    }

    "fail to validate on empty Map" in {

      TransactionRecord.formRule.validate(Map.empty) must
        be(Failure(Seq((Path \ "isRecorded") -> Seq(ValidationError("error.required.ba.select.transaction.record")))))

    }

    "fail to validate  invalid data" in {

      val model = Map(
        "isRecorded" -> Seq("true"),
        "transactions[]" -> Seq("01, 10")
      )
      TransactionRecord.formRule.validate(model) must
        be(Failure(Seq((Path \ "transactions") -> Seq(ValidationError("error.invalid")))))

    }

    "validate form write for valid transaction record" in {

      val map = Map(
        "isRecorded" -> Seq("true"),
        "transactions[]" -> Seq("03","01"),
        "name" -> Seq("test")
      )

      val model = TransactionRecordYes(Set(DigitalSoftware("test"), Paper))
     TransactionRecord.formWrites.writes(model) must be (map)
    }

    "validate form write for option No" in {

      val map = Map(
        "isRecorded" -> Seq("false")
      )
      val model = TransactionRecordNo
      TransactionRecord.formWrites.writes(model) must be (map)
    }

    "validate form write for option Yes" in {

      val map = Map(
        "isRecorded" -> Seq("true"),
        "transactions[]" -> Seq("02","01")
      )

      val model = TransactionRecordYes(Set(DigitalSpreadsheet, Paper))
      TransactionRecord.formWrites.writes(model) must be (map)
    }

    "form write test" in {
      val map = Map(
        "isRecorded" -> Seq("false")
      )
      val model = TransactionRecordNo

      TransactionRecord.formWrites.writes(model) must be(map)
    }

    "JSON validation" must {

      "successfully validate given values" in {
        val json =  Json.obj("isRecorded" -> true,
          "transactions" -> Seq("01","02"))

        Json.fromJson[TransactionRecord](json) must
          be(JsSuccess(TransactionRecordYes(Set(Paper, DigitalSpreadsheet)), JsPath \ "isRecorded" \ "transactions"))
      }

      "successfully validate given values with option No" in {
        val json =  Json.obj("isRecorded" -> false)

        Json.fromJson[TransactionRecord](json) must
          be(JsSuccess(TransactionRecordNo, JsPath \ "isRecorded"))
      }

      "successfully validate given values with option Digital software" in {
        val json =  Json.obj("isRecorded" -> true,
          "transactions" -> Seq("03", "02"),
        "digitalSoftwareName" -> "test")

        Json.fromJson[TransactionRecord](json) must
          be(JsSuccess(TransactionRecordYes(Set(DigitalSoftware("test"), DigitalSpreadsheet)), JsPath \ "isRecorded" \ "transactions" \ "digitalSoftwareName"))
      }

      "fail when on path is missing" in {
        Json.fromJson[TransactionRecord](Json.obj("isRecorded" -> true,
          "transaction" -> Seq("01"))) must
          be(JsError((JsPath \ "isRecorded" \ "transactions") -> play.api.data.validation.ValidationError("error.path.missing")))
      }

      "fail when on invalid data" in {
        Json.fromJson[TransactionRecord](Json.obj("isRecorded" -> true,"transactions" -> Seq("40"))) must
          be(JsError(((JsPath \ "isRecorded" \ "transactions") \ "transactions") -> play.api.data.validation.ValidationError("error.invalid")))
      }

      "write valid data in using json write" in {
        Json.toJson[TransactionRecord](TransactionRecordYes(Set(Paper, DigitalSoftware("test657")))) must be (Json.obj("isRecorded" -> true,
        "transactions" -> Seq("01", "03"),
          "digitalSoftwareName" -> "test657"
        ))
      }

      "write valid data in using json write with Option No" in {
        Json.toJson[TransactionRecord](TransactionRecordNo) must be (Json.obj("isRecorded" -> false))
      }
    }
  }
}


