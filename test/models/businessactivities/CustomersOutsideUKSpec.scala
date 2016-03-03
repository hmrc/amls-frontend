package models.businessactivities

import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsNull, JsPath, JsSuccess, Json}

class CustomersOutsideUKSpec extends PlaySpec {


  "CustomersOutsideUK" must {

    "successfully validate the form Rule with option No" in {
      CustomersOutsideUK.formRule.validate(Map("isOutside" -> Seq("false"))) must
        be(Success(CustomersOutsideUKNo))
    }

    "successfully validate the form Rule with option Yes" in {
      CustomersOutsideUK.formRule.validate(Map("isOutside" -> Seq("true"),
      "country_1" -> Seq("GS"))) must
        be(Success(CustomersOutsideUKYes(Countries("GS"))))
    }

    "validate mandatory field when isOutside is not selected" in {
      CustomersOutsideUK.formRule.validate(Map.empty) must
        be(Failure(Seq(
          (Path \ "isOutside") -> Seq(ValidationError("error.required"))
        )))
    }

    "validate mandatory field when isOutside is  selected as Yes and country with invalid data" in {
      val json = Map("isOutside" -> Seq("true"),
      "country_1" -> Seq("ABC"))

      CustomersOutsideUK.formRule.validate(json) must
        be(Failure(Seq(
          (Path \ "country_1") -> Seq(ValidationError("error.maxLength", 2))
        )))
    }

    "validate mandatory country field" in {
      CustomersOutsideUK.formRule.validate(Map("isOutside" -> Seq("true"))) must
        be(Failure(Seq(
          (Path \ "country_1") -> Seq(ValidationError("error.required"))
        )))
    }

    "successfully write model with formWrite" in {

      val model = CustomersOutsideUKYes(Countries("GP"))
      CustomersOutsideUK.formWrites.writes(model) must be (Map("isRecorded" -> Seq("true"),
        "country_1" -> Seq("GP")))

    }

    "JSON validation" must {
      "successfully validate givcen values" in {
        val json =  Json.obj("isOutside" -> true,
          "country_1" -> "GP")

        Json.fromJson[CustomersOutsideUK](json) must
          be(JsSuccess(CustomersOutsideUKYes(Countries("GP")), JsPath \ "isOutside"))
      }

      "successfully validate given values with option No" in {
        val json =  Json.obj("isOutside" -> false)

        Json.fromJson[CustomersOutsideUK](json) must
          be(JsSuccess(CustomersOutsideUKNo, JsPath \ "isOutside"))
      }

      "write valid data in using json write" in {
        Json.toJson[CustomersOutsideUK](CustomersOutsideUKYes(Countries("GS"))) must be (Json.obj("isOutside" -> true,
          "country_1" -> "GS",
          "country_2" -> JsNull,
          "country_3" -> JsNull,
          "country_4" -> JsNull,
          "country_5" -> JsNull,
          "country_6" -> JsNull,
          "country_7" -> JsNull,
          "country_8" -> JsNull,
          "country_9" -> JsNull,
          "country_10" ->JsNull
        ))
      }
    }
  }
}
