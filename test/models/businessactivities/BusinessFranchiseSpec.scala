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

package models.businessactivities

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class BusinessFranchiseSpec extends PlaySpec with MockitoSugar {

  "JSON validation" must {
    "successfully validate given an enum value" in {

      Json.fromJson[BusinessFranchise](Json.obj("businessFranchise" -> false)) must
        be(JsSuccess(BusinessFranchiseNo, JsPath))
    }

    "successfully validate given an `Yes` value" in {

      val json = Json.obj("businessFranchise" -> true, "franchiseName" -> "test test")

      Json.fromJson[BusinessFranchise](json) must
        be(JsSuccess(BusinessFranchiseYes("test test"), JsPath \ "franchiseName"))
    }

    "fail to validate when given an empty `Yes` value" in {

      val json = Json.obj("businessFranchise" -> true)

      Json.fromJson[BusinessFranchise](json) must
        be(JsError((JsPath \ "franchiseName") -> play.api.libs.json.JsonValidationError("error.path.missing")))
    }

    "write the correct value" in {

      Json.toJson(BusinessFranchiseNo.asInstanceOf[BusinessFranchise]) must
        be(Json.obj("businessFranchise" -> false))

      Json.toJson(BusinessFranchiseYes("test test").asInstanceOf[BusinessFranchise]) must
        be(
          Json.obj(
            "businessFranchise" -> true,
            "franchiseName"     -> "test test"
          )
        )
    }
  }

}
