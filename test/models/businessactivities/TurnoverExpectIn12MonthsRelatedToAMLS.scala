package models.businessactivities

import models.aboutyou.InternalAccountant
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class TurnoverExpectIn12MonthsRelatedToAMLSSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {

    "successfully validate given an enum value" in {

      TurnerOverExpectIn12MonthsRelatedToAMLS.formRule.validate(Map("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> Seq("01"))) must
        be(Success(FirstTurnoverAmls))

      TurnerOverExpectIn12MonthsRelatedToAMLS.formRule.validate(Map("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> Seq("02"))) must
        be(Success(SecondTurnoverAmls))

      TurnerOverExpectIn12MonthsRelatedToAMLS.formRule.validate(Map("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> Seq("03"))) must
        be(Success(ThirdTurnoverAmls))

      TurnerOverExpectIn12MonthsRelatedToAMLS.formRule.validate(Map("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> Seq("04"))) must
        be(Success(FourthTurnoverAmls))

      TurnerOverExpectIn12MonthsRelatedToAMLS.formRule.validate(Map("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> Seq("05"))) must
        be(Success(FivthTurnoverAmls))

      TurnerOverExpectIn12MonthsRelatedToAMLS.formRule.validate(Map("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> Seq("06"))) must
        be(Success(SixthTurnoverAmls))

      TurnerOverExpectIn12MonthsRelatedToAMLS.formRule.validate(Map("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> Seq("07"))) must
        be(Success(SevenTurnoverAmls))
    }


    }

    "write correct data from enum value" in {

      TurnerOverExpectIn12MonthsRelatedToAMLS.formWrites.writes(FirstTurnoverAmls) must
        be(Map("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> Seq("01")))

      TurnerOverExpectIn12MonthsRelatedToAMLS.formWrites.writes(SecondTurnoverAmls) must
        be(Map("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> Seq("02")))

      TurnerOverExpectIn12MonthsRelatedToAMLS.formWrites.writes(ThirdTurnoverAmls) must
        be(Map("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> Seq("03")))

      TurnerOverExpectIn12MonthsRelatedToAMLS.formWrites.writes(FourthTurnoverAmls) must
        be(Map("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> Seq("04")))

      TurnerOverExpectIn12MonthsRelatedToAMLS.formWrites.writes(FivthTurnoverAmls) must
        be(Map("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> Seq("05")))

      TurnerOverExpectIn12MonthsRelatedToAMLS.formWrites.writes(SixthTurnoverAmls) must
        be(Map("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> Seq("06")))

      TurnerOverExpectIn12MonthsRelatedToAMLS.formWrites.writes(SevenTurnoverAmls) must
        be(Map("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> Seq("07")))
    }

  "JSON validation" must {

    "successfully validate given an enum value" in {

      Json.fromJson[TurnerOverExpectIn12MonthsRelatedToAMLS](Json.obj("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "01")) must
        be(JsSuccess(FirstTurnoverAmls, JsPath \ "turnoverOverExpectIn12MOnthsRelatedToAMLS"))

      Json.fromJson[TurnerOverExpectIn12MonthsRelatedToAMLS](Json.obj("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "02")) must
        be(JsSuccess(SecondTurnoverAmls, JsPath \ "turnoverOverExpectIn12MOnthsRelatedToAMLS"))

      Json.fromJson[TurnerOverExpectIn12MonthsRelatedToAMLS](Json.obj("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "03")) must
        be(JsSuccess(ThirdTurnoverAmls, JsPath \ "turnoverOverExpectIn12MOnthsRelatedToAMLS"))

      Json.fromJson[TurnerOverExpectIn12MonthsRelatedToAMLS](Json.obj("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "04")) must
        be(JsSuccess(FourthTurnoverAmls, JsPath \ "turnoverOverExpectIn12MOnthsRelatedToAMLS"))

      Json.fromJson[TurnerOverExpectIn12MonthsRelatedToAMLS](Json.obj("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "05")) must
        be(JsSuccess(FivthTurnoverAmls, JsPath \ "turnoverOverExpectIn12MOnthsRelatedToAMLS"))

      Json.fromJson[TurnerOverExpectIn12MonthsRelatedToAMLS](Json.obj("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "06")) must
        be(JsSuccess(SixthTurnoverAmls, JsPath \ "turnoverOverExpectIn12MOnthsRelatedToAMLS"))

      Json.fromJson[TurnerOverExpectIn12MonthsRelatedToAMLS](Json.obj("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "07")) must
        be(JsSuccess(SevenTurnoverAmls, JsPath \ "turnoverOverExpectIn12MOnthsRelatedToAMLS"))
    }


    "write the correct value" in {

      Json.toJson(FirstTurnoverAmls) must
        be(Json.obj("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "01"))

      Json.toJson(SecondTurnoverAmls) must
        be(Json.obj("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "02"))

      Json.toJson(ThirdTurnoverAmls) must
        be(Json.obj("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "03"))

      Json.toJson(FourthTurnoverAmls) must
        be(Json.obj("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "04"))

      Json.toJson(FivthTurnoverAmls) must
        be(Json.obj("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "05"))

      Json.toJson(SixthTurnoverAmls) must
        be(Json.obj("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "06"))

      Json.toJson(SevenTurnoverAmls) must
        be(Json.obj("turnoverOverExpectIn12MOnthsRelatedToAMLS" -> "07"))


    }
  }
}
