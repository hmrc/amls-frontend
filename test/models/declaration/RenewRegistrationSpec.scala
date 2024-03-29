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

package models.declaration

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, JsSuccess, Json}

class RenewRegistrationSpec extends PlaySpec with MockitoSugar {

  "JSON validation" must {
    "successfully validate given an enum value" in {

      Json.fromJson[RenewRegistration](Json.obj("renewRegistration" -> false)) must
        be(JsSuccess(RenewRegistrationNo, JsPath))
    }

    "successfully validate given an `Yes` value" in {
      val json = Json.obj("renewRegistration" -> true)

      Json.fromJson[RenewRegistration](json) must
        be(JsSuccess(RenewRegistrationYes))
    }

    "successfully validate given an `No` value" in {
      val json = Json.obj("renewRegistration" -> false)

      Json.fromJson[RenewRegistration](json) must
        be(JsSuccess(RenewRegistrationNo))
    }

    "write the correct value" in {
      Json.toJson(RenewRegistrationNo.asInstanceOf[RenewRegistration]) must
        be(Json.obj("renewRegistration" -> false))

      Json.toJson(RenewRegistrationYes.asInstanceOf[RenewRegistration]) must
        be(Json.obj("renewRegistration" -> true))
    }
  }
}
