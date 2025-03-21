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

class VATRegisteredSpec extends PlaySpec with MockitoSugar {

  "JSON validation" must {

    "successfully validate given an enum value" in {

      Json.fromJson[VATRegistered](Json.obj("registeredForVAT" -> false)) must
        be(JsSuccess(VATRegisteredNo, JsPath))
    }

    "successfully validate given an `Yes` value" in {

      val json = Json.obj("registeredForVAT" -> true, "vrnNumber" -> "12345678")

      Json.fromJson[VATRegistered](json) must
        be(JsSuccess(VATRegisteredYes("12345678"), JsPath \ "vrnNumber"))
    }

    "fail to validate when given an empty `Yes` value" in {

      val json = Json.obj("registeredForVAT" -> true)

      Json.fromJson[VATRegistered](json) must
        be(JsError((JsPath \ "vrnNumber") -> play.api.libs.json.JsonValidationError("error.path.missing")))
    }

    "write the correct value" in {

      Json.toJson(VATRegisteredNo.asInstanceOf[VATRegistered]) must
        be(Json.obj("registeredForVAT" -> false))

      Json.toJson(VATRegisteredYes("12345678").asInstanceOf[VATRegistered]) must
        be(
          Json.obj(
            "registeredForVAT" -> true,
            "vrnNumber"        -> "12345678"
          )
        )
    }
  }

}
