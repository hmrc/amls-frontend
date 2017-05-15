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

package models.businessactivities

import models.FormTypes
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class BusinessFranchiseSpec extends PlaySpec with MockitoSugar {

  "Form Validation" must {
    "successfully validate given an enum value" in {
      BusinessFranchise.formRule.validate(Map("businessFranchise" -> Seq("false"))) must
        be(Valid(BusinessFranchiseNo))
    }

    "successfully validate given an `Yes` value" in {
      val data = Map(
        "businessFranchise" -> Seq("true"),
        "franchiseName" -> Seq("test test")
      )

      BusinessFranchise.formRule.validate(data) must
        be(Valid(BusinessFranchiseYes("test test")))
    }

    "fail to validate given an `Yes` with no value" when {
      "represented by an empty string" in {
        val data = Map(
          "businessFranchise" -> Seq("true"),
          "franchiseName" -> Seq("")
        )

        BusinessFranchise.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "franchiseName") -> Seq(ValidationError("error.required.ba.franchise.name"))
          )))
      }

      "represented by a missing field" in {
        val data = Map(
          "businessFranchise" -> Seq("true")
        )

        BusinessFranchise.formRule.validate(data) must
          be(Invalid(Seq(
            (Path \ "franchiseName") -> Seq(ValidationError("error.required"))
          )))
      }
    }

    "fail to validate given a franchise name that is too long" in {

      val data = Map(
        "businessFranchise" -> Seq("true"),
        "franchiseName" -> Seq("test test"*50)
      )

      BusinessFranchise.formRule.validate(data) must
        be(Invalid(Seq(
          (Path \ "franchiseName") -> Seq(ValidationError("error.max.length.ba.franchise.name"))
        )))
    }

    "fail to validate given a franchise name containing invalid characters" in {

      val data = Map(
        "businessFranchise" -> Seq("true"),
        "franchiseName" -> Seq("test{}test1")
      )

      BusinessFranchise.formRule.validate(data) must
        be(Invalid(Seq(
          (Path \ "franchiseName") -> Seq(ValidationError("err.text.validation"))
        )))
    }

    "fail to validate when neither 'yes' nor 'no' are selected" when {
      val reps = Seq[(String, Map[String, Seq[String]])] (
        "missing field" -> Map.empty[String, Seq[String]],
        "empty string" -> Map("businessFranchise" -> Seq("")),
        "invalid data" -> Map("businessFranchise" -> Seq("NOTVALID"))
      )

      reps.foreach {
        case (desc, rep) =>
        s"represented by $desc" in {
          val data = rep
            BusinessFranchise.formRule.validate(data) must
              be(Invalid(Seq(
                (Path \ "businessFranchise") -> Seq(ValidationError("error.required.ba.is.your.franchise"))
              )))
        }
      }
    }

    "write correct data from enum value" in {

      BusinessFranchise.formWrites.writes(BusinessFranchiseNo) must
        be(Map("businessFranchise" -> Seq("false")))

    }

    "write correct data from `Yes` value" in {
      BusinessFranchise.formWrites.writes(BusinessFranchiseYes("test test")) must
        be(Map("businessFranchise" -> Seq("true"), "franchiseName" -> Seq("test test")))
    }

  }

  "JSON validation" must {
    "successfully validate given an enum value" in {

      Json.fromJson[BusinessFranchise](Json.obj("businessFranchise" -> false)) must
        be(JsSuccess(BusinessFranchiseNo, JsPath))
    }

    "successfully validate given an `Yes` value" in {

      val json = Json.obj("businessFranchise" -> true, "franchiseName" ->"test test")

      Json.fromJson[BusinessFranchise](json) must
        be(JsSuccess(BusinessFranchiseYes("test test"), JsPath \ "franchiseName"))
    }

    "fail to validate when given an empty `Yes` value" in {

      val json = Json.obj("businessFranchise" -> true)

      Json.fromJson[BusinessFranchise](json) must
        be(JsError((JsPath \ "franchiseName") -> play.api.data.validation.ValidationError("error.path.missing")))
    }

    "write the correct value" in {

      Json.toJson(BusinessFranchiseNo) must
        be(Json.obj("businessFranchise" -> false))

      Json.toJson(BusinessFranchiseYes("test test")) must
        be(Json.obj(
          "businessFranchise" -> true,
          "franchiseName" -> "test test"
        ))
    }
  }

}
