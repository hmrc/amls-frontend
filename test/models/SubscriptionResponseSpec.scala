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

class SubscriptionResponseSpec extends PlaySpec {

  "SubscriptionResponse" must {

    "deserialize correctly from previous model" in {

      val previousJson =
        """{"etmpFormBundleNumber":"bundle",
          |"amlsRefNo":"XDML00000000000",
          |"registrationFee":0,
          |"fpFee":100,
          |"fpFeeRate":100,
          |"premiseFee":345,
          |"premiseFeeRate":115,
          |"totalFees":445,
          |"paymentReference":"XT000000000000"}""".stripMargin

      val response = SubscriptionResponse("bundle", "XDML00000000000",
        Some(SubscriptionFees("XT000000000000", 0, Some(100), Some(100), 345, Some(115), 445)))

      SubscriptionResponse.reads.reads(Json.parse(previousJson)) must be(JsSuccess(response))

    }

    "deserialize correctly from current model" in {

      val previousJson =
        """{"etmpFormBundleNumber":"bundle",
          |"amlsRefNo":"XDML00000000000",
          |"subscriptionFees" : {
          |"registrationFee":0,
          |"fpFee":100,
          |"fpFeeRate":100,
          |"premiseFee":345,
          |"premiseFeeRate":115,
          |"totalFees":445,
          |"paymentReference":"XT000000000000"
          |}}""".stripMargin

      val response = SubscriptionResponse("bundle", "XDML00000000000",
        Some(SubscriptionFees("XT000000000000", 0, Some(100), Some(100), 345, Some(115), 445)))

      SubscriptionResponse.reads.reads(Json.parse(previousJson)) must be(JsSuccess(response))

    }

    "deserialize correctly when no fees" in {

      val previousJson =
        """{"etmpFormBundleNumber":"bundle",
          |"amlsRefNo":"XDML00000000000",
          |"previouslySubmitted":true}""".stripMargin

      val response = SubscriptionResponse("bundle", "XDML00000000000",
        None, Some(true))

      SubscriptionResponse.reads.reads(Json.parse(previousJson)) must be(JsSuccess(response))

    }
  }
}
