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

package models.estateagentbusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json._

class RedressSchemeSpec extends PlaySpec with MockitoSugar {

  "RedressScheemsSpec" must {

    "validate model with redress option selected as yes" in {

      RedressScheme.formRedressRule.validate(Map("isRedress" -> Seq("true"), "propertyRedressScheme" -> Seq("01"))) must
        be(Valid(ThePropertyOmbudsman))

      RedressScheme.formRedressRule.validate(Map("isRedress" -> Seq("true"), "propertyRedressScheme" -> Seq("02"))) must
        be(Valid(OmbudsmanServices))

      RedressScheme.formRedressRule.validate(Map("isRedress" -> Seq("true"), "propertyRedressScheme" -> Seq("03"))) must
        be(Valid(PropertyRedressScheme))

      RedressScheme.formRedressRule.validate(Map("isRedress" -> Seq("true"), "propertyRedressScheme" -> Seq("04"), "other" -> Seq("test"))) must
        be(Valid(Other("test")))

    }

    "validate model redress option selected as No" in {
      val model = Map(
        "isRedress" -> Seq("false"),
        "propertyRedressScheme" -> Seq("02")
      )

      RedressScheme.formRedressRule.validate(model) must
        be(Valid(RedressSchemedNo))
    }

    "fail to validate given an `other` with no value" in {

      val data = Map(
        "isRedress" -> Seq("true"),
        "propertyRedressScheme" -> Seq("04"),
          "other" -> Seq("")
      )

      RedressScheme.formRedressRule.validate(data) must
        be(Invalid(Seq(
          (Path \ "other") -> Seq(ValidationError("error.required.eab.redress.scheme.name"))
        )))
    }

    "fail to validate given an `other` with max value" in {

      val data = Map(
        "isRedress" -> Seq("true"),
        "propertyRedressScheme" -> Seq("04"),
        "other" -> Seq("asadasas"*50)
      )

      RedressScheme.formRedressRule.validate(data) must
        be(Invalid(Seq(
          (Path \ "other") -> Seq(ValidationError("error.invalid.eab.redress.scheme.name"))
        )))
    }


    "fail to validate given a non-enum value" in {

      val data = Map(
        "isRedress" -> Seq("true"),
        "propertyRedressScheme" -> Seq("10")
      )

      RedressScheme.formRedressRule.validate(data) must
        be(Invalid(Seq(
          (Path \ "propertyRedressScheme") -> Seq(ValidationError("error.invalid"))
        )))
    }

    "fail to validate given an empty form post" in {
      val data = Map[String, Seq[String]]()

      RedressScheme.formRedressRule.validate(data) must be(Invalid(Seq(
        (Path \ "isRedress") -> Seq(ValidationError("error.required.eab.redress.scheme"))
      )))
    }

    "fail to validate given an empty value for 'propertyRedressScheme'" in {
      val data = Map(
        "isRedress" -> Seq("true")
      )

      RedressScheme.formRedressRule.validate(data) must be(Invalid(Seq(
        (Path \ "propertyRedressScheme") -> Seq(ValidationError("error.required.eab.which.redress.scheme"))
      )))
    }

    "write correct data from enum value" in {

      RedressScheme.formRedressWrites.writes(ThePropertyOmbudsman) must
        be(Map("isRedress" -> Seq("true"),"propertyRedressScheme" -> Seq("01")))

      RedressScheme.formRedressWrites.writes(OmbudsmanServices) must
        be(Map("isRedress" -> Seq("true"),"propertyRedressScheme" -> Seq("02")))

      RedressScheme.formRedressWrites.writes(PropertyRedressScheme) must
        be(Map("isRedress" -> Seq("true"),"propertyRedressScheme" -> Seq("03")))

      RedressScheme.formRedressWrites.writes(Other("foobar")) must
        be(Map("isRedress" -> Seq("true"),"propertyRedressScheme" -> Seq("04"), "other" -> Seq("foobar")))

      RedressScheme.formRedressWrites.writes(RedressSchemedNo) must
        be(Map("isRedress" -> Seq("false")))
    }

    "JSON validation" must {
      "successfully validate selecting redress option no" in {

        Json.fromJson[RedressScheme](Json.obj("isRedress"-> false)) must
          be(JsSuccess(RedressSchemedNo, JsPath))

      }

      "successfully validate json Reads" in {
        Json.fromJson[RedressScheme](Json.obj("isRedress"-> true,"propertyRedressScheme" -> "01")) must
          be(JsSuccess(ThePropertyOmbudsman, JsPath))

        Json.fromJson[RedressScheme](Json.obj("isRedress"-> true,"propertyRedressScheme" -> "02")) must
          be(JsSuccess(OmbudsmanServices, JsPath ))

        Json.fromJson[RedressScheme](Json.obj("isRedress"-> true,"propertyRedressScheme" -> "03")) must
          be(JsSuccess(PropertyRedressScheme, JsPath))

        val json = Json.obj("isRedress"-> true,
                            "propertyRedressScheme" -> "04",
                            "propertyRedressSchemeOther" -> "test")

        Json.fromJson[RedressScheme](json) must
          be(JsSuccess(Other("test"), JsPath \ "propertyRedressSchemeOther"))

        Json.fromJson[RedressScheme](Json.obj("isRedress"-> false)) must
          be(JsSuccess(RedressSchemedNo, JsPath))

      }

      "fail to validate when given an empty `other` value" in {

        val json = Json.obj("isRedress"-> true,
                             "propertyRedressScheme" -> "04"
                            )

        Json.fromJson[RedressScheme](json) must
          be(JsError((JsPath \"propertyRedressSchemeOther") -> play.api.data.validation.ValidationError("error.path.missing")))
      }

      "fail to validate when invalid option is passed" in {

        val json = Json.obj("isRedress"-> true,
          "propertyRedressScheme" -> "10"
        )

        Json.fromJson[RedressScheme](json) must
          be(JsError((JsPath) -> play.api.data.validation.ValidationError("error.invalid")))
      }


      "successfully validate json write" in {

        Json.toJson(ThePropertyOmbudsman) must be(Json.obj("isRedress"-> true, "propertyRedressScheme" -> "01"))

        Json.toJson(OmbudsmanServices) must be(Json.obj("isRedress"-> true, "propertyRedressScheme" -> "02"))

        Json.toJson(PropertyRedressScheme) must be(Json.obj("isRedress"-> true, "propertyRedressScheme" -> "03"))

        val json = Json.obj("isRedress"-> true,
          "propertyRedressScheme" -> "04",
          "propertyRedressSchemeOther" -> "test")

        Json.toJson(Other("test")) must be(json)

        Json.toJson(RedressSchemedNo) must be(Json.obj("isRedress"-> false))
      }
    }
  }
}
