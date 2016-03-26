package models.businessactivities

import models.FormTypes
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Path, Failure, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsNull, JsPath, JsSuccess, Json}

class CustomersOutsideUKSpec extends PlaySpec {


  "CustomersOutsideUK" must {

    "validate toLines for UK address" in {
      CustomersOutsideUKYes(Countries("GP")).toLines must be (Seq("GP"))
    }

    "validate toLines for Non UK address" in {
      CustomersOutsideUKNo.toLines mustBe Seq.empty
    }

    "successfully validate the form Rule with option No" in {
      CustomersOutsideUK.formRule.validate(Map("isOutside" -> Seq("false"))) must
        be(Success(CustomersOutsideUKNo))
    }

    "successfully validate the form Rule with option Yes" in {
      CustomersOutsideUK.formRule.validate(Map("isOutside" -> Seq("true"),
      "country_1" -> Seq("GS"),
        "country_2" -> Seq("AR"),
        "country_3" -> Seq("AB"),
        "country_4" -> Seq("AC"),
        "country_5" -> Seq("AD"),
        "country_6" -> Seq("AE"),
        "country_7" -> Seq("AF"),
        "country_8" -> Seq("AG"),
        "country_9" -> Seq("AH"),
        "country_10" -> Seq("AI")
      )) must
        be(Success(CustomersOutsideUKYes(Countries("GS", Some("AR"), Some("AB"), Some("AC"),
          Some("AD"), Some("AE"), Some("AF"), Some("AG"), Some("AH"),Some("AI")))))
    }

    "validate mandatory field when isOutside is not selected" in {
      CustomersOutsideUK.formRule.validate(Map.empty) must
        be(Failure(Seq(
          (Path \ "isOutside") -> Seq(ValidationError("error.required.ba.select.country"))
        )))
    }

    "validate mandatory field when isOutside is  selected as Yes and country with invalid data" in {
      val json = Map("isOutside" -> Seq("true"),
      "country_1" -> Seq("ABC"))

      CustomersOutsideUK.formRule.validate(json) must
        be(Failure(Seq(
          (Path \ "country_1") -> Seq(ValidationError("error.invalid.country", FormTypes.countryRegex))
        )))
    }

    "validate mandatory country field" in {
      CustomersOutsideUK.formRule.validate(Map("isOutside" -> Seq("true"),"country_1" -> Seq(""))) must
        be(Failure(Seq(
          (Path \ "country_1") -> Seq(ValidationError("error.required.ba.country.name"))
        )))
    }

    "successfully write model with formWrite" in {

      val model = CustomersOutsideUKYes(Countries("GP", Some("AB")))
      CustomersOutsideUK.formWrites.writes(model) must
        contain allOf (
        "isOutside" -> Seq("true"),
        "country_1" -> Seq("GP"),
        "country_2" -> Seq("AB")
        )
    }


    "successfully write model with formWrite and option No" in {

      val model = CustomersOutsideUKNo
      CustomersOutsideUK.formWrites.writes(model) mustBe Map("isOutside" -> Seq("false"))
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

      "write valid data using json write" in {
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

      "write valid data using json write with option No" in {
        Json.toJson[CustomersOutsideUK](CustomersOutsideUKNo) must be (Json.obj("isOutside" -> false))
      }
    }
  }
}
