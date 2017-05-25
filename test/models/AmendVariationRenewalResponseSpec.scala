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

package models


import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}

class AmendVariationRenewalResponseSpec extends PlaySpec {

  val response = AmendVariationRenewalResponse("pdate", "12345", 115.0, Some(125.0), Some(115.0), 0, None, 240.0, Some("ref"), None)

  "AmendVariationResponse" must {

    "Deserialize correctly where Fit and Proper Fee is fpFee" in {

      val json =
        """{
  "processingDate" : "pdate",
  "etmpFormBundleNumber" : "12345",
  "registrationFee" : 115.0,
  "fpFee" : 125.0,
  "premiseFee" : 0,
  "totalFees" : 240.0,
  "paymentReference" : "ref",
  "addedResponsiblePeople" : 0,
  "addedResponsiblePeopleFitAndProper" : 0,
  "addedFullYearTradingPremises" : 0,
  "halfYearlyTradingPremises" : 0,
  "zeroRatedTradingPremises" : 0
}"""

      AmendVariationRenewalResponse.format.reads(Json.parse(json)) must be(JsSuccess(response.copy(fpFeeRate = None)))

    }

    "Deserialize correctly where Fit and Proper Fee is not returned" in {

      val json =
        """{
  "processingDate" : "pdate",
  "etmpFormBundleNumber" : "12345",
  "registrationFee" : 115.0,
  "premiseFee" : 0,
  "totalFees" : 240.0,
  "paymentReference" : "ref",
  "addedResponsiblePeople" : 0,
  "addedResponsiblePeopleFitAndProper" : 0,
  "addedFullYearTradingPremises" : 0,
  "halfYearlyTradingPremises" : 0,
  "zeroRatedTradingPremises" : 0
}"""

      AmendVariationRenewalResponse.format.reads(Json.parse(json)) must be(JsSuccess(response.copy(fpFee = None, fpFeeRate = None)))

    }
  }
}
