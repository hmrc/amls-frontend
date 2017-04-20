package models.responsiblepeople

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json._

class TimeAtAddressSpec extends PlaySpec with MockitoSugar {

  val FieldName = "timeAtAddress"

  "Form Validation" must {

    val ZeroToFiveForm = Map(FieldName -> Seq("01"))
    val SixToElevenForm = Map(FieldName -> Seq("02"))
    val OneToThreeForm = Map(FieldName -> Seq("03"))
    val MoreThanThreeForm = Map(FieldName -> Seq("04"))

    "successfully validate given an enum value" in {
      TimeAtAddress.formRule.validate(ZeroToFiveForm) must be(Valid(TimeAtAddress.ZeroToFiveMonths))
      TimeAtAddress.formRule.validate(SixToElevenForm) must be(Valid(TimeAtAddress.SixToElevenMonths))
      TimeAtAddress.formRule.validate(OneToThreeForm) must be(Valid(TimeAtAddress.OneToThreeYears))
      TimeAtAddress.formRule.validate(MoreThanThreeForm) must be(Valid(TimeAtAddress.ThreeYearsPlus))
    }

    "write correct data from enum value" in {
      TimeAtAddress.formWrites.writes(TimeAtAddress.ZeroToFiveMonths) must be(ZeroToFiveForm)
      TimeAtAddress.formWrites.writes(TimeAtAddress.SixToElevenMonths) must be(SixToElevenForm)
      TimeAtAddress.formWrites.writes(TimeAtAddress.OneToThreeYears) must be(OneToThreeForm)
      TimeAtAddress.formWrites.writes(TimeAtAddress.ThreeYearsPlus) must be(MoreThanThreeForm)
    }

    "throw error on invalid data" in {
      TimeAtAddress.formRule.validate(Map(FieldName -> Seq("20"))) must
        be(Invalid(Seq((Path \ FieldName, Seq(ValidationError("error.invalid"))))))
    }

    "throw error on empty data" in {
      TimeAtAddress.formRule.validate(Map.empty) must
        be(Invalid(Seq((Path \ FieldName, Seq(ValidationError("error.required.timeAtAddress"))))))
    }
  }

  "JSON validation" must {

    val ZeroToFiveJson = Json.obj(FieldName -> "01")
    val SixToElevenJson = Json.obj(FieldName -> "02")
    val OneToThreeJson = Json.obj(FieldName -> "03")
    val MoreThanThreeJson = Json.obj(FieldName -> "04")

    "successfully validate given an enum value" in {

      Json.fromJson[TimeAtAddress](ZeroToFiveJson) must be(JsSuccess(TimeAtAddress.ZeroToFiveMonths, JsPath))
      Json.fromJson[TimeAtAddress](SixToElevenJson) must be(JsSuccess(TimeAtAddress.SixToElevenMonths, JsPath))
      Json.fromJson[TimeAtAddress](OneToThreeJson) must be(JsSuccess(TimeAtAddress.OneToThreeYears, JsPath))
      Json.fromJson[TimeAtAddress](MoreThanThreeJson) must be(JsSuccess(TimeAtAddress.ThreeYearsPlus, JsPath))
    }

    "write the correct value" in {
      Json.toJson(TimeAtAddress.ZeroToFiveMonths) must be(ZeroToFiveJson)
      Json.toJson(TimeAtAddress.SixToElevenMonths) must be(SixToElevenJson)
      Json.toJson(TimeAtAddress.OneToThreeYears) must be(OneToThreeJson)
      Json.toJson(TimeAtAddress.ThreeYearsPlus) must be(MoreThanThreeJson)
    }

    "throw error for invalid data" in {
      Json.fromJson[TimeAtAddress](Json.obj(FieldName -> "20")) must
        be(JsError(JsPath, play.api.data.validation.ValidationError("error.invalid")))
    }
  }
}
