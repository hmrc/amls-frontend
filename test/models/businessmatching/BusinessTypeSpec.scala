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

import models.businessmatching.BusinessType._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsString, JsSuccess, Json}

class BusinessTypeSpec extends PlaySpec with MockitoSugar {

  "BusinessType" when {

    "writing JSON" must {
      "convert to json from model" in {
        Json.toJson(BusinessType.SoleProprietor.asInstanceOf[BusinessType]) must
          be(JsString("Sole Trader"))

        Json.toJson(BusinessType.LimitedCompany.asInstanceOf[BusinessType]) must
          be(JsString("Corporate Body"))

        Json.toJson(BusinessType.Partnership.asInstanceOf[BusinessType]) must
          be(JsString("Partnership"))

        Json.toJson(BusinessType.LPrLLP.asInstanceOf[BusinessType]) must
          be(JsString("LLP"))

        Json.toJson(BusinessType.UnincorporatedBody.asInstanceOf[BusinessType]) must
          be(JsString("Unincorporated Body"))
      }
    }

    "reading JSON" must {
      "convert from json to model" in {

        Json.fromJson[BusinessType](JsString("Sole Trader"))         must
          be(JsSuccess(SoleProprietor))

        Json.fromJson[BusinessType](JsString("Corporate Body"))      must
          be(JsSuccess(LimitedCompany))

        Json.fromJson[BusinessType](JsString("Partnership"))         must
          be(JsSuccess(Partnership))

        Json.fromJson[BusinessType](JsString("LLP"))                 must
          be(JsSuccess(LPrLLP))

        Json.fromJson[BusinessType](JsString("Unincorporated Body")) must
          be(JsSuccess(UnincorporatedBody))
      }
    }
  }
}
