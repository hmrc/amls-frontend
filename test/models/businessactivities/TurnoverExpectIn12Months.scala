package models.businessactivities

import models.aboutyou.InternalAccountant
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class TurnoverExpectIn12MonthsSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "successfully validate given an enum value" in {

      TurnerOverExpectIn12Months.formRule.validate(Map("turnoverOverExpectIn12MOnths" -> Seq("01"))) must
        be(Success(First))

      TurnerOverExpectIn12Months.formRule.validate(Map("turnoverOverExpectIn12MOnths" -> Seq("02"))) must
        be(Success(Second))

      TurnerOverExpectIn12Months.formRule.validate(Map("turnoverOverExpectIn12MOnths" -> Seq("03"))) must
        be(Success(Third))

      TurnerOverExpectIn12Months.formRule.validate(Map("turnoverOverExpectIn12MOnths" -> Seq("04"))) must
        be(Success(Fourth))

      TurnerOverExpectIn12Months.formRule.validate(Map("turnoverOverExpectIn12MOnths" -> Seq("05"))) must
        be(Success(Five))

      TurnerOverExpectIn12Months.formRule.validate(Map("turnoverOverExpectIn12MOnths" -> Seq("06"))) must
        be(Success(Six))

      TurnerOverExpectIn12Months.formRule.validate(Map("turnoverOverExpectIn12MOnths" -> Seq("07"))) must
        be(Success(Seven))
    }


    }

    "write correct data from enum value" in {

      TurnerOverExpectIn12Months.formWrites.writes(First) must
        be(Map("turnoverOverExpectIn12MOnths" -> Seq("01")))

      TurnerOverExpectIn12Months.formWrites.writes(Second) must
        be(Map("turnoverOverExpectIn12MOnths" -> Seq("02")))

      TurnerOverExpectIn12Months.formWrites.writes(Third) must
        be(Map("turnoverOverExpectIn12MOnths" -> Seq("03")))

      TurnerOverExpectIn12Months.formWrites.writes(Fourth) must
        be(Map("turnoverOverExpectIn12MOnths" -> Seq("04")))

      TurnerOverExpectIn12Months.formWrites.writes(Five) must
        be(Map("turnoverOverExpectIn12MOnths" -> Seq("05")))

      TurnerOverExpectIn12Months.formWrites.writes(Six) must
        be(Map("turnoverOverExpectIn12MOnths" -> Seq("06")))

      TurnerOverExpectIn12Months.formWrites.writes(Seven) must
        be(Map("turnoverOverExpectIn12MOnths" -> Seq("07")))
    }

  "JSON validation" must {

    "successfully validate given an enum value" in {

      Json.fromJson[TurnerOverExpectIn12Months](Json.obj("turnoverOverExpectIn12MOnths" -> "01")) must
        be(JsSuccess(First, JsPath \ "turnoverOverExpectIn12MOnths"))

      Json.fromJson[TurnerOverExpectIn12Months](Json.obj("turnoverOverExpectIn12MOnths" -> "02")) must
        be(JsSuccess(Second, JsPath \ "turnoverOverExpectIn12MOnths"))

      Json.fromJson[TurnerOverExpectIn12Months](Json.obj("turnoverOverExpectIn12MOnths" -> "03")) must
        be(JsSuccess(Third, JsPath \ "turnoverOverExpectIn12MOnths"))

      Json.fromJson[TurnerOverExpectIn12Months](Json.obj("turnoverOverExpectIn12MOnths" -> "04")) must
        be(JsSuccess(Fourth, JsPath \ "turnoverOverExpectIn12MOnths"))

      Json.fromJson[TurnerOverExpectIn12Months](Json.obj("turnoverOverExpectIn12MOnths" -> "05")) must
        be(JsSuccess(Five, JsPath \ "turnoverOverExpectIn12MOnths"))

      Json.fromJson[TurnerOverExpectIn12Months](Json.obj("turnoverOverExpectIn12MOnths" -> "06")) must
        be(JsSuccess(Six, JsPath \ "turnoverOverExpectIn12MOnths"))

      Json.fromJson[TurnerOverExpectIn12Months](Json.obj("turnoverOverExpectIn12MOnths" -> "07")) must
        be(JsSuccess(Seven, JsPath \ "turnoverOverExpectIn12MOnths"))
    }


    "write the correct value" in {

      Json.toJson(First) must
        be(Json.obj("turnoverOverExpectIn12MOnths" -> "01"))

      Json.toJson(Second) must
        be(Json.obj("turnoverOverExpectIn12MOnths" -> "02"))

      Json.toJson(Third) must
        be(Json.obj("turnoverOverExpectIn12MOnths" -> "03"))

      Json.toJson(Fourth) must
        be(Json.obj("turnoverOverExpectIn12MOnths" -> "04"))

      Json.toJson(Five) must
        be(Json.obj("turnoverOverExpectIn12MOnths" -> "05"))

      Json.toJson(Six) must
        be(Json.obj("turnoverOverExpectIn12MOnths" -> "06"))

      Json.toJson(Seven) must
        be(Json.obj("turnoverOverExpectIn12MOnths" -> "07"))


    }
  }
}
