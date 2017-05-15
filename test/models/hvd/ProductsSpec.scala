/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models.hvd

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}


class ProductsSpec extends PlaySpec with MockitoSugar {

  "Products" must {

    "validate model with few check box selected" in {

      val model = Map(
        "products[]" -> Seq("01", "02" ,"12"),
        "otherDetails" -> Seq("test")
      )

      Products.formRule.validate(model) must
        be(Valid(Products(Set(Alcohol, Tobacco, Other("test")))))

    }

    "fail validation when 'Other' is selected but no details are provided" when {
      "represented by an empty string" in {
        val model = Map("products[]" -> Seq("12"),
                        "otherDetails" -> Seq(""))

        Products.formRule.validate(model) must
          be(Invalid(List((Path \ "otherDetails", Seq(ValidationError("error.required.hvd.business.sell.other.details"))))))
      }

      "represented by a sequence of whitespace" in {
        val model = Map("products[]" -> Seq("12"),
          "otherDetails" -> Seq("  \t"))

        Products.formRule.validate(model) must
          be(Invalid(List((Path \ "otherDetails", Seq(ValidationError("error.required.hvd.business.sell.other.details"))))))
      }

      "represented by a missing field" in {
        val model = Map("products[]" -> Seq("12"))
        Products.formRule.validate(model) must
          be(Invalid(List((Path \ "otherDetails", Seq(ValidationError("error.required"))))))
      }

      "contains invalid characters in the 'other details' field" in {
        val model = Map(
          "products[]" -> Seq("12"),
          "otherDetails" -> Seq("¡93u4jk<>{}"))

        Products.formRule.validate(model) must be(
          Invalid(List((Path \ "otherDetails", Seq(ValidationError("err.text.validation")))))
        )
      }
    }

    "fail validation when field otherDetails exceeds maximum length" in {

      val model = Map(
        "products[]" -> Seq("01", "02" ,"03", "04", "05", "12"),
        "otherDetails" -> Seq("t"*256)
      )
      Products.formRule.validate(model) must
        be(Invalid(List(( Path \ "otherDetails", Seq(ValidationError("error.invalid.hvd.business.sell.other.details"))))))
    }


    "fail validation when none of the check boxes are selected" when {
      List(
        "empty list" -> Map("products[]" -> Seq(),"otherDetails" -> Seq("test")),
        "missing field" -> Map.empty[String, Seq[String]]
      ).foreach { x =>
        val (rep, model) = x
        s"represented by $rep" in {
          Products.formRule.validate(model) must
            be(Invalid(List((Path \ "products", List(ValidationError("error.required.hvd.business.sell.atleast"))))))
        }
      }
    }

    "fail to validate  invalid data" in {
      val model = Map(
        "products[]" -> Seq("01, 15")
      )
      Products.formRule.validate(model) must
        be(Invalid(Seq((Path \ "products") -> Seq(ValidationError("error.invalid")))))

    }

    "validate form write for valid transaction record" in {

      val map = Map(
        "products[]" -> Seq("12","08"),
        "otherDetails" -> Seq("test")
      )

      val model = Products(Set(Other("test"), Gold))
      Products.formWrites.writes(model) must be (map)
    }

    "validate form write for option Yes" in {

      val map = Map(
        "products[]" -> Seq("08", "11", "10", "02", "01")
      )

      val model = Products(Set(Clothing, MobilePhones, Gold, Alcohol, Tobacco))
      Products.formWrites.writes(model) must be (map)
    }

    "JSON validation" must {

      "successfully validate given values" in {
        val json =  Json.obj(
          "products" -> Seq("06","07", "08", "02", "01", "11"))

        Json.fromJson[Products](json) must
          be(JsSuccess(Products(Set(Clothing, Jewellery, Alcohol, Caravans, Gold, Tobacco)), JsPath))
      }
      "successfully validate given all values" in {
        val json =  Json.obj(
          "products" -> Seq("01","02","03","04","05","06","07","08","09","10","11"))

        Json.fromJson[Products](json) must
          be(JsSuccess(Products(Set(
            MobilePhones,
            Clothing,
            Jewellery,
            ScrapMetals,
            Alcohol,
            Caravans,
            Gold,
            Tobacco,
            Antiques,
            Cars,
            OtherMotorVehicles)), JsPath))
      }

      "successfully validate given values with option other details" in {
        val json =  Json.obj(
          "products" -> Seq("09", "12"),
        "otherDetails" -> "test")

        Json.fromJson[Products](json) must
          be(JsSuccess(Products(Set(Other("test"), ScrapMetals)), JsPath))
      }

      "fail when on path is missing" in {
        Json.fromJson[Products](Json.obj(
          "product" -> Seq("01"))) must
          be(JsError((JsPath \ "products") -> play.api.data.validation.ValidationError("error.path.missing")))
      }

      "fail when on invalid data" in {
        Json.fromJson[Products](Json.obj("products" -> Seq("40"))) must
          be(JsError(((JsPath) \ "products") -> play.api.data.validation.ValidationError("error.invalid")))
      }

      "write valid data in using json write" in {
        Json.toJson[Products](Products(Set(Tobacco, Other("test657")))) must be (
        Json.obj("products" -> Json.arr("02", "12"),
          "otherDetails" -> "test657"
        ))
      }
    }
  }
}



