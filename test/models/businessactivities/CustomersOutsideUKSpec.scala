package models.businessactivities

import models.Country
import models.FormTypes
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsNull, JsPath, JsSuccess, Json}

class CustomersOutsideUKSpec extends PlaySpec {


  "CustomersOutsideUK" must {

    "validate toLines for UK address" in {
      CustomersOutsideUKYes(Countries(Country("United Kingdom", "GB"))).toLines must be (Seq("United Kingdom"))
    }

    "validate toLines for Non UK address" in {
      CustomersOutsideUKNo.toLines mustBe Seq.empty
    }

    "successfully validate the form Rule with option No" in {
      CustomersOutsideUK.formRule.validate(Map("isOutside" -> Seq("false"))) must
        be(Success(CustomersOutsideUKNo))
    }

    "successfully validate the form Rule with option Yes" in {
      CustomersOutsideUK.formRule.validate(
        Map(
          "isOutside" -> Seq("true"),
          "country_1" -> Seq("AL"),
          "country_2" -> Seq("DZ"),
          "country_3" -> Seq("AS"),
          "country_4" -> Seq("AD"),
          "country_5" -> Seq("AO"),
          "country_6" -> Seq("AI"),
          "country_7" -> Seq("AQ"),
          "country_8" -> Seq("AG"),
          "country_9" -> Seq("AR"),
          "country_10" -> Seq("AM")
        )
      ) mustBe {
        Success(CustomersOutsideUKYes(
          Countries(
            Country("Albania", "AL"),
            Some(Country("Algeria", "DZ")),
            Some(Country("American Samoa", "AS")),
            Some(Country("Andorra", "AD")),
            Some(Country("Angola", "AO")),
            Some(Country("Anguilla", "AI")),
            Some(Country("Antarctica", "AQ")),
            Some(Country("Antigua and Barbuda", "AG")),
            Some(Country("Argentina", "AR")),
            Some(Country("Armenia", "AM"))
          )
        ))
      }
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
          (Path \ "country_1") -> Seq(ValidationError("error.required.ba.country.name"))
        )))
    }

    "validate mandatory field for min length when isOutside is  selected as Yes and country with invalid data" in {
      val json = Map("isOutside" -> Seq("true"),
        "country_1" -> Seq("A"))

      CustomersOutsideUK.formRule.validate(json) must
        be(Failure(Seq(
          (Path \ "country_1") -> Seq(ValidationError("error.required.ba.country.name"))
        )))
    }

    "validate mandatory country field" in {
      CustomersOutsideUK.formRule.validate(Map("isOutside" -> Seq("true"),"country_1" -> Seq(""))) must
        be(Failure(Seq(
          (Path \ "country_1") -> Seq(ValidationError("error.required.ba.country.name"))
        )))
    }

    "successfully write model with formWrite" in {

      val model = CustomersOutsideUKYes(Countries(Country("United Kingdom", "GB"), Some(Country("United Kingdom", "GB"))))
      CustomersOutsideUK.formWrites.writes(model) must
        contain allOf (
        "isOutside" -> Seq("true"),
        "country_1" -> Seq("GB"),
        "country_2" -> Seq("GB")
        )
    }


    "successfully write model with formWrite and option No" in {

      val model = CustomersOutsideUKNo
      CustomersOutsideUK.formWrites.writes(model) mustBe Map("isOutside" -> Seq("false"))
    }

    "JSON validation" must {
      "successfully validate givcen values" in {
        val json =  Json.obj("isOutside" -> true,
          "country_1" -> "GB")

        Json.fromJson[CustomersOutsideUK](json) must
          be(JsSuccess(CustomersOutsideUKYes(Countries(Country("United Kingdom", "GB"))), JsPath \ "isOutside"))
      }

      "successfully validate given values with option No" in {
        val json =  Json.obj("isOutside" -> false)

        Json.fromJson[CustomersOutsideUK](json) must
          be(JsSuccess(CustomersOutsideUKNo, JsPath \ "isOutside"))
      }

      "write valid data using json write" in {
        Json.toJson[CustomersOutsideUK](CustomersOutsideUKYes(Countries(Country("United Kingdom", "GB")))) must be (Json.obj("isOutside" -> true,
          "country_1" -> "GB",
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
