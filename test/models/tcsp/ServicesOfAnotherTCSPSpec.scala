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

package models.tcsp

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, JsSuccess, Json}

class ServicesOfAnotherTCSPSpec extends PlaySpec with MockitoSugar {

  "services of another Tcsp" must {

    "JSON validation" when {

      "successfully validate given an enum value" in {

        Json.fromJson[ServicesOfAnotherTCSP](Json.obj("servicesOfAnotherTCSP" -> false)) must
          be(JsSuccess(ServicesOfAnotherTCSPNo, JsPath))
      }

      "successfully validate given an `Yes` value" in {

        Json.fromJson[ServicesOfAnotherTCSP](
          Json.obj("servicesOfAnotherTCSP" -> true, "mlrRefNumber" -> "12345678")
        ) must
          be(JsSuccess(ServicesOfAnotherTCSPYes(Some("12345678")), JsPath \ "mlrRefNumber"))
      }

      "write the correct value" in {

        Json.toJson(ServicesOfAnotherTCSPNo.asInstanceOf[ServicesOfAnotherTCSP]) must
          be(Json.obj("servicesOfAnotherTCSP" -> false))

        Json.toJson(ServicesOfAnotherTCSPYes(Some("12345678")).asInstanceOf[ServicesOfAnotherTCSP]) must
          be(
            Json.obj(
              "servicesOfAnotherTCSP" -> true,
              "mlrRefNumber"          -> "12345678"
            )
          )
      }
    }
  }
}
