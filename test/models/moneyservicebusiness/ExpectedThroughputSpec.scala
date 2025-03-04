/*
 * Copyright 2024 HM Revenue & Customs
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

package models.moneyservicebusiness

import models.renewal.TotalThroughput
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class ExpectedThroughputSpec extends PlaySpec with Matchers {

  "JSON validation" must {

    "successfully validate given an enum value" in {

      Json.fromJson[ExpectedThroughput](Json.obj("throughput" -> "01")) must
        be(JsSuccess(ExpectedThroughput.First, JsPath))

      Json.fromJson[ExpectedThroughput](Json.obj("throughput" -> "02")) must
        be(JsSuccess(ExpectedThroughput.Second, JsPath))

      Json.fromJson[ExpectedThroughput](Json.obj("throughput" -> "03")) must
        be(JsSuccess(ExpectedThroughput.Third, JsPath))

      Json.fromJson[ExpectedThroughput](Json.obj("throughput" -> "04")) must
        be(JsSuccess(ExpectedThroughput.Fourth, JsPath))

      Json.fromJson[ExpectedThroughput](Json.obj("throughput" -> "05")) must
        be(JsSuccess(ExpectedThroughput.Fifth, JsPath))

      Json.fromJson[ExpectedThroughput](Json.obj("throughput" -> "06")) must
        be(JsSuccess(ExpectedThroughput.Sixth, JsPath))

      Json.fromJson[ExpectedThroughput](Json.obj("throughput" -> "07")) must
        be(JsSuccess(ExpectedThroughput.Seventh, JsPath))
    }

    "write the correct value" in {
      Json.toJson(ExpectedThroughput.First.asInstanceOf[ExpectedThroughput]) must
        be(Json.obj("throughput" -> "01"))

      Json.toJson(ExpectedThroughput.Second.asInstanceOf[ExpectedThroughput]) must
        be(Json.obj("throughput" -> "02"))

      Json.toJson(ExpectedThroughput.Third.asInstanceOf[ExpectedThroughput]) must
        be(Json.obj("throughput" -> "03"))

      Json.toJson(ExpectedThroughput.Fourth.asInstanceOf[ExpectedThroughput]) must
        be(Json.obj("throughput" -> "04"))

      Json.toJson(ExpectedThroughput.Fifth.asInstanceOf[ExpectedThroughput]) must
        be(Json.obj("throughput" -> "05"))

      Json.toJson(ExpectedThroughput.Sixth.asInstanceOf[ExpectedThroughput]) must
        be(Json.obj("throughput" -> "06"))

      Json.toJson(ExpectedThroughput.Seventh.asInstanceOf[ExpectedThroughput]) must
        be(Json.obj("throughput" -> "07"))
    }

    "throw error for invalid data" in {
      Json.fromJson[ExpectedThroughput](Json.obj("throughput" -> "20")) must
        be(JsError(JsPath, play.api.libs.json.JsonValidationError("error.invalid")))
    }
  }

  "convert to renewal throughput model" in {
    ExpectedThroughput.convert(ExpectedThroughput.First)   must be(TotalThroughput("01"))
    ExpectedThroughput.convert(ExpectedThroughput.Second)  must be(TotalThroughput("02"))
    ExpectedThroughput.convert(ExpectedThroughput.Third)   must be(TotalThroughput("03"))
    ExpectedThroughput.convert(ExpectedThroughput.Fourth)  must be(TotalThroughput("04"))
    ExpectedThroughput.convert(ExpectedThroughput.Fifth)   must be(TotalThroughput("05"))
    ExpectedThroughput.convert(ExpectedThroughput.Sixth)   must be(TotalThroughput("06"))
    ExpectedThroughput.convert(ExpectedThroughput.Seventh) must be(TotalThroughput("07"))
  }

}
