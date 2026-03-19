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
          |"approvalCheckFee": 150,
          |"approvalCheckFeeRate": 150,
          |"premiseFee":345,
          |"premiseFeeRate":115,
          |"totalFees":445,
          |"paymentReference":"XT000000000000"}""".stripMargin

      val response = SubscriptionResponse(
        "bundle",
        "XDML00000000000",
        Some(SubscriptionFees("XT000000000000", 0, Some(100), Some(100), Some(150), Some(150), 345, Some(115), 445))
      )

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

      val response = SubscriptionResponse(
        "bundle",
        "XDML00000000000",
        Some(SubscriptionFees("XT000000000000", 0, Some(100), Some(100), None, None, 345, Some(115), 445))
      )

      SubscriptionResponse.reads.reads(Json.parse(previousJson)) must be(JsSuccess(response))

    }

    "deserialize correctly when no fees" in {

      val previousJson =
        """{"etmpFormBundleNumber":"bundle",
          |"amlsRefNo":"XDML00000000000",
          |"previouslySubmitted":true}""".stripMargin

      val response = SubscriptionResponse("bundle", "XDML00000000000", None, Some(true))

      SubscriptionResponse.reads.reads(Json.parse(previousJson)) must be(JsSuccess(response))

    }
  }

  "SubscriptionResponse methods" must {

    val fees = SubscriptionFees(
      paymentReference    = "XT000000000000",
      registrationFee     = BigDecimal(100),
      fpFee               = Some(BigDecimal(50)),
      fpFeeRate           = Some(BigDecimal(10)),
      approvalCheckFee    = Some(BigDecimal(25)),
      approvalCheckFeeRate = Some(BigDecimal(5)),
      premiseFee          = BigDecimal(200),
      premiseFeeRate      = Some(BigDecimal(115)),
      totalFees           = BigDecimal(375)
    )

    val responseWithFees    = SubscriptionResponse("bundle", "XDML00000000000", Some(fees))
    val responseWithoutFees = SubscriptionResponse("bundle", "XDML00000000000", None)

    "getRegistrationFee returns fee when subscriptionFees is defined" in {
      responseWithFees.getRegistrationFee mustBe BigDecimal(100)
    }

    "getRegistrationFee returns 0 when subscriptionFees is None" in {
      responseWithoutFees.getRegistrationFee mustBe BigDecimal(0)
    }

    "getPremiseFee returns fee when subscriptionFees is defined" in {
      responseWithFees.getPremiseFee mustBe BigDecimal(200)
    }

    "getPremiseFee returns 0 when subscriptionFees is None" in {
      responseWithoutFees.getPremiseFee mustBe BigDecimal(0)
    }

    "getTotalFees returns fee when subscriptionFees is defined" in {
      responseWithFees.getTotalFees mustBe BigDecimal(375)
    }

    "getTotalFees returns 0 when subscriptionFees is None" in {
      responseWithoutFees.getTotalFees mustBe BigDecimal(0)
    }

    "getPaymentReference returns reference when subscriptionFees is defined" in {
      responseWithFees.getPaymentReference mustBe "XT000000000000"
    }

    "getPaymentReference returns empty string when subscriptionFees is None" in {
      responseWithoutFees.getPaymentReference mustBe ""
    }

    "getPremiseFeeRate returns rate when subscriptionFees is defined" in {
      responseWithFees.getPremiseFeeRate mustBe Some(BigDecimal(115))
    }

    "getPremiseFeeRate returns None when subscriptionFees is None" in {
      responseWithoutFees.getPremiseFeeRate mustBe None
    }

    "getFpFeeRate returns rate when subscriptionFees is defined" in {
      responseWithFees.getFpFeeRate mustBe Some(BigDecimal(10))
    }

    "getFpFeeRate returns None when subscriptionFees is None" in {
      responseWithoutFees.getFpFeeRate mustBe None
    }

    "getFpFee returns fee when subscriptionFees is defined" in {
      responseWithFees.getFpFee mustBe Some(BigDecimal(50))
    }

    "getFpFee returns None when subscriptionFees is None" in {
      responseWithoutFees.getFpFee mustBe None
    }

    "getApprovalCheckFee returns fee when subscriptionFees is defined" in {
      responseWithFees.getApprovalCheckFee mustBe Some(BigDecimal(25))
    }

    "getApprovalCheckFee returns None when subscriptionFees is None" in {
      responseWithoutFees.getApprovalCheckFee mustBe None
    }

    "getApprovalCheckFeeRate returns rate when subscriptionFees is defined" in {
      responseWithFees.getApprovalCheckFeeRate mustBe Some(BigDecimal(5))
    }

    "getApprovalCheckFeeRate returns None when subscriptionFees is None" in {
      responseWithoutFees.getApprovalCheckFeeRate mustBe None
    }

    "serialise to JSON using format" in {
      Json.toJson(responseWithFees).as[SubscriptionResponse] mustEqual responseWithFees
    }
  }
}
