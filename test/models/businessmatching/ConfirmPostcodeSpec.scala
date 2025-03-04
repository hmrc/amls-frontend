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

package models.businessmatching

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, JsSuccess, Json}

class ConfirmPostcodeSpec extends PlaySpec with MockitoSugar {

  "ConfirmPostcode" must {

    "Json validation" must {
      "READ the JSON successfully and return the domain Object" in {
        val postCode            = ConfirmPostcode("AA11AA")
        val jsonConfirmPostcode = Json.obj("postCode" -> "AA11AA")
        val fromJson            = Json.fromJson[ConfirmPostcode](jsonConfirmPostcode)
        fromJson must be(JsSuccess(postCode, JsPath))
      }

      "read write json successfully" in {
        val postCode = ConfirmPostcode("AA11AA")
        ConfirmPostcode.format.reads(ConfirmPostcode.format.writes(postCode)) must
          be(JsSuccess(postCode, JsPath))
      }
    }
  }
}
