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

package models.businessmatching

import models.Country
import models.businesscustomer.ReviewDetails
import models.businesscustomer.Address
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => eqTo, _}

class ReviewDetailsSpec extends PlaySpec with MockitoSugar {
  suite =>

  val model = ReviewDetails(
    businessName = "Name",
    businessType = Some(BusinessType.SoleProprietor),
    businessAddress = Address(
      "1 Test Street", "Test Town", None, None, None, Country("United Kingdom", "GB")
    ),
    safeId = "safeId"
  )

  val json = Json.obj(
    "businessName" -> "Name",
    "businessType" -> "Sole Trader",
    "businessAddress" -> Json.obj(
      "line_1" -> "1 Test Street",
      "line_2" -> "Test Town",
      "country" -> "GB"
    ),
    "safeId" -> "safeId"
  )

  "Review Details Model" must {

    "validate correctly with a `Some` business type" in {

      Json.fromJson[ReviewDetails](json) mustEqual JsSuccess(model)
      Json.toJson(model) mustEqual (json)
    }

    "validate successfully with an invalid business type" in {

      val model = suite.model.copy(
        businessType = None
      )

      val json = Json.obj(
        "businessName" -> "Name",
        "businessType" -> "corporate body",
        "businessAddress" -> Json.obj(
          "line_1" -> "1 Test Street",
          "line_2" -> "Test Town",
          "country" -> "GB"
        ),
        "safeId" -> "safeId"
      )

      Json.fromJson[ReviewDetails](json) mustEqual JsSuccess(model)
    }

    "validate correctly with a `None` business type" in {

      val model = suite.model.copy(businessType = None)

      val json = Json.obj(
        "businessName" -> "Name",
        "businessAddress" -> Json.obj(
          "line_1" -> "1 Test Street",
          "line_2" -> "Test Town",
          "country" -> "GB"
        ),
        "safeId" -> "safeId"
      )

      Json.fromJson[ReviewDetails](json) mustEqual JsSuccess(model)
    }
  }
}
