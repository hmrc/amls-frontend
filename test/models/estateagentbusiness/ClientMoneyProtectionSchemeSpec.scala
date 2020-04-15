/*
 * Copyright 2020 HM Revenue & Customs
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

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, JsSuccess, Json}

class ClientMoneyProtectionSchemeSpec extends PlaySpec with MockitoSugar {

  "JSON validation" must {

    "successfully validate given an enum value" in {

      Json.fromJson[ClientMoneyProtectionScheme](Json.obj("clientMoneyProtection" -> false)) must
        be(JsSuccess(ClientMoneyProtectionSchemeNo, JsPath ))
    }

    "successfully validate given an `Yes` value" in {

      val json = Json.obj("clientMoneyProtection" -> true)

      Json.fromJson[ClientMoneyProtectionScheme](json) must
        be(JsSuccess(ClientMoneyProtectionSchemeYes))
    }

    "write the correct value" in {

      Json.toJson(ClientMoneyProtectionSchemeNo) must
        be(Json.obj("clientMoneyProtection" -> false))

      Json.toJson(ClientMoneyProtectionSchemeYes) must
        be(Json.obj("clientMoneyProtection" -> true))
    }
  }
}
