/*
 * Copyright 2018 HM Revenue & Customs
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

package models

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}
import SatisfactionSurvey._
class SatisfactionSurveySpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {


    "successfully validate given feedback with empty details" in {
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("01"), "details" -> Seq(""))) must
        be(Valid(First(None)))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("02"), "details" -> Seq(""))) must
        be(Valid(Second(None)))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("03"), "details" -> Seq(""))) must
        be(Valid(Third(None)))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("04"), "details" -> Seq(""))) must
        be(Valid(Fourth(None)))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("05"), "details" -> Seq(""))) must
        be(Valid(Fifth(None)))
    }

    "successfully validate given feedback with details" in {
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("01"), "details" -> Seq("123"))) must
        be(Valid(First(Some("123"))))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("02"), "details" -> Seq("123"))) must
        be(Valid(Second(Some("123"))))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("03"), "details" -> Seq("123"))) must
        be(Valid(Third(Some("123"))))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("04"), "details" -> Seq("123"))) must
        be(Valid(Fourth(Some("123"))))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("05"), "details" -> Seq("123"))) must
        be(Valid(Fifth(Some("123"))))
    }

    "successfully validate missing mandatory details" in {
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("01"))) must
        be(Valid(First(None)))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("02"))) must
        be(Valid(Second(None)))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("03"))) must
        be(Valid(Third(None)))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("04"))) must
        be(Valid(Fourth(None)))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("05"))) must
        be(Valid(Fifth(None)))
    }

    "fail to validate missing mandatory satisfaction" in {
      val data = Map(
        "details" -> Seq("")
      )
      SatisfactionSurvey.formRule.validate(data) must
        be(Invalid(Seq(
          (Path \ "satisfaction") -> Seq(ValidationError("error.survey.satisfaction.required"))
        )))
    }

    "fail to validate empty data" in {
      SatisfactionSurvey.formRule.validate(Map.empty) must
        be(Invalid(Seq(
          (Path \ "satisfaction") -> Seq(ValidationError("error.survey.satisfaction.required"))
        )))
    }

    "fail to validate details over max value" in {
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("01"),"details" -> Seq("zzxczxczx"*150))) must
        be(Invalid(Seq(
          (Path \ "details") -> Seq(ValidationError("error.invalid.maxlength.1200"))
        )))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("02"),"details" -> Seq("zzxczxczx"*150))) must
        be(Invalid(Seq(
          (Path \ "details") -> Seq(ValidationError("error.invalid.maxlength.1200"))
        )))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("03"),"details" -> Seq("zzxczxczx"*150))) must
        be(Invalid(Seq(
          (Path \ "details") -> Seq(ValidationError("error.invalid.maxlength.1200"))
        )))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("04"),"details" -> Seq("zzxczxczx"*150))) must
        be(Invalid(Seq(
          (Path \ "details") -> Seq(ValidationError("error.invalid.maxlength.1200"))
        )))
      SatisfactionSurvey.formRule.validate(Map("satisfaction" -> Seq("05"),"details" -> Seq("zzxczxczx"*150))) must
        be(Invalid(Seq(
          (Path \ "details") -> Seq(ValidationError("error.invalid.maxlength.1200"))
        )))
    }

  }

  "JSON validation" must {

    "successfully validate given feedback with empty details" in {
      Json.fromJson[SatisfactionSurvey](Json.obj("satisfaction" -> "01")) must
        be(JsSuccess(First(None), JsPath))
      Json.fromJson[SatisfactionSurvey](Json.obj("satisfaction" -> "02")) must
        be(JsSuccess(Second(None), JsPath))
      Json.fromJson[SatisfactionSurvey](Json.obj("satisfaction" -> "03")) must
        be(JsSuccess(Third(None), JsPath))
      Json.fromJson[SatisfactionSurvey](Json.obj("satisfaction" -> "04")) must
        be(JsSuccess(Fourth(None), JsPath))
      Json.fromJson[SatisfactionSurvey](Json.obj("satisfaction" -> "05")) must
        be(JsSuccess(Fifth(None), JsPath))
    }

    "successfully validate given feedback with details" in {
      Json.fromJson[SatisfactionSurvey](Json.obj("satisfaction" -> "01", "details" ->"123")) must
        be(JsSuccess(First(Some("123")), JsPath \ "details"))
      Json.fromJson[SatisfactionSurvey](Json.obj("satisfaction" -> "02", "details" ->"123")) must
        be(JsSuccess(Second(Some("123")), JsPath \ "details"))
      Json.fromJson[SatisfactionSurvey](Json.obj("satisfaction" -> "03", "details" ->"123")) must
        be(JsSuccess(Third(Some("123")), JsPath \ "details"))
      Json.fromJson[SatisfactionSurvey](Json.obj("satisfaction" -> "04", "details" ->"123")) must
        be(JsSuccess(Fourth(Some("123")), JsPath \ "details"))
      Json.fromJson[SatisfactionSurvey](Json.obj("satisfaction" -> "05", "details" ->"123")) must
        be(JsSuccess(Fifth(Some("123")), JsPath \ "details"))

    }

    "fail to validate given no data" in {
      val json = Json.obj()
      Json.fromJson[SatisfactionSurvey](json) must
        be(JsError((JsPath \ "satisfaction") -> play.api.data.validation.ValidationError("error.path.missing")))
    }

  }

  "write the correct value" in {
    Json.toJson(First(None)) must
      be(Json.obj("satisfaction" -> "01", "details" -> ""))
    Json.toJson(First(Some("123"))) must
      be(Json.obj(
        "satisfaction" -> "01",
        "details" -> "123"
      ))

    Json.toJson(Second(None)) must
      be(Json.obj("satisfaction" -> "02", "details" -> ""))
    Json.toJson(Second(Some("123"))) must
      be(Json.obj(
        "satisfaction" -> "02",
        "details" -> "123"
      ))

    Json.toJson(Third(None)) must
      be(Json.obj("satisfaction" -> "03", "details" -> ""))
    Json.toJson(Third(Some("123"))) must
      be(Json.obj(
        "satisfaction" -> "03",
        "details" -> "123"
      ))

    Json.toJson(Fourth(None)) must
      be(Json.obj("satisfaction" -> "04", "details" -> ""))
    Json.toJson(Fourth(Some("123"))) must
      be(Json.obj(
        "satisfaction" -> "04",
        "details" -> "123"
      ))

    Json.toJson(Fifth(None)) must
      be(Json.obj("satisfaction" -> "05", "details" -> ""))
    Json.toJson(Fifth(Some("123"))) must
      be(Json.obj(
        "satisfaction" -> "05",
        "details" -> "123"
      ))
  }


}
