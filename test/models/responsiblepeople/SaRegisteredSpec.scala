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

package models.responsiblepeople

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class SaRegisteredSpec extends PlaySpec with MockitoSugar {

  "JSON validation" must {
    "successfully validate given an enum value" in {

      Json.fromJson[SaRegistered](Json.obj("saRegistered" -> false)) must
        be(JsSuccess(SaRegisteredNo, JsPath))
    }

    "successfully validate given an `Yes` value" in {

      val json = Json.obj("saRegistered" -> true, "utrNumber" -> "0123456789")

      Json.fromJson[SaRegistered](json) must
        be(JsSuccess(SaRegisteredYes("0123456789"), JsPath \ "utrNumber"))
    }

    "fail to validate when given an empty `Yes` value" in {

      val json = Json.obj("saRegistered" -> true)

      Json.fromJson[SaRegistered](json) must
        be(JsError((JsPath \ "utrNumber") -> play.api.libs.json.JsonValidationError("error.path.missing")))
    }

    "write the correct value" in {

      Json.toJson(SaRegisteredNo.asInstanceOf[SaRegistered]) must
        be(Json.obj("saRegistered" -> false))

      Json.toJson(SaRegisteredYes("0123456789").asInstanceOf[SaRegistered]) must
        be(
          Json.obj(
            "saRegistered" -> true,
            "utrNumber"    -> "0123456789"
          )
        )
    }
  }

}
