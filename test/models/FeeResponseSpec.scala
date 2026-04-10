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

import models.ResponseType.{AmendOrVariationResponseType, SubscriptionResponseType}
import models.status._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsString, Json}

import java.time.LocalDateTime
import java.time.zone.ZoneOffsetTransitionRule.TimeDefinition.UTC

class FeeResponseSpec extends PlaySpec {

  "FeeResponse.toPay" must {
    val difference  = BigDecimal(100)
    val feeResponse = FeeResponse(
      AmendOrVariationResponseType,
      "XAML00000567890",
      BigDecimal(100),
      Some(BigDecimal(100)),
      None,
      BigDecimal(100),
      BigDecimal(100),
      None,
      Some(difference),
      LocalDateTime.of(2018, 1, 1, 0, 0)
    )
    "return difference" when {
      "status is RenewalSubmitted and submissionRequestStatus is renewal amendment" in {
        feeResponse.toPay(
          RenewalSubmitted(None),
          Some(SubmissionRequestStatus(true, isRenewalAmendment = Some(true)))
        ) mustEqual difference
      }

      "status is ReadyForRenewal and submissionRequestStatus is renewal amendment" in {
        feeResponse.toPay(
          ReadyForRenewal(None),
          Some(SubmissionRequestStatus(true, isRenewalAmendment = Some(true)))
        ) mustEqual difference
      }

      "status is SubmissionReadyForReview and responseType is AmendOrVariationResponseType" in {
        feeResponse.toPay(
          SubmissionReadyForReview,
          Some(SubmissionRequestStatus(true))
        ) mustEqual difference
      }
    }

    "return 0" when {
      val feeResponse = FeeResponse(
        AmendOrVariationResponseType,
        "XAML00000567890",
        BigDecimal(100),
        Some(BigDecimal(100)),
        None,
        BigDecimal(100),
        BigDecimal(100),
        None,
        None,
        LocalDateTime.of(2018, 1, 1, 0, 0)
      )
      "status is RenewalSubmitted and submissionRequestStatus is renewal amendment and difference is None" in {
        feeResponse.toPay(
          RenewalSubmitted(None),
          Some(SubmissionRequestStatus(true, isRenewalAmendment = Some(true)))
        ) mustEqual BigDecimal(0)
      }

      "status is ReadyForRenewal and submissionRequestStatus is renewal amendment and difference is None" in {
        feeResponse.toPay(
          ReadyForRenewal(None),
          Some(SubmissionRequestStatus(true, isRenewalAmendment = Some(true)))
        ) mustEqual BigDecimal(0)
      }

      "status is SubmissionReadyForReview and responseType is AmendOrVariationResponseType and difference is None" in {
        feeResponse.toPay(
          SubmissionReadyForReview,
          Some(SubmissionRequestStatus(true))
        ) mustEqual BigDecimal(0)
      }
    }

    "return total" when {
      val total       = BigDecimal(100)
      val feeResponse = FeeResponse(
        AmendOrVariationResponseType,
        "XAML00000567890",
        BigDecimal(100),
        Some(BigDecimal(100)),
        None,
        BigDecimal(100),
        total,
        None,
        Some(difference),
        LocalDateTime.of(2018, 1, 1, 0, 0)
      )

      "status is RenewalSubmitted and submissionRequestStatus is not renewal amendment" in {
        feeResponse.toPay(
          RenewalSubmitted(None),
          Some(SubmissionRequestStatus(true, isRenewalAmendment = Some(false)))
        ) mustEqual total
      }

      "status is ReadyForRenewal and submissionRequestStatus is not renewal amendment" in {
        feeResponse.toPay(
          ReadyForRenewal(None),
          Some(SubmissionRequestStatus(true, isRenewalAmendment = Some(false)))
        ) mustEqual total
      }

      "status is RenewalSubmitted and submissionRequestStatus does not set renewal amendment" in {
        feeResponse.toPay(
          RenewalSubmitted(None),
          Some(SubmissionRequestStatus(true, isRenewalAmendment = None))
        ) mustEqual total
      }

      "status is ReadyForRenewal and submissionRequestStatus does not set renewal amendment" in {
        feeResponse.toPay(
          ReadyForRenewal(None),
          Some(SubmissionRequestStatus(true, isRenewalAmendment = None))
        ) mustEqual total
      }

      "status is SubmissionReadyForReview and responseType is SubscriptionResponseType" in {
        val feeResponse = FeeResponse(
          SubscriptionResponseType,
          "XAML00000567890",
          BigDecimal(100),
          Some(BigDecimal(100)),
          None,
          BigDecimal(100),
          total,
          None,
          Some(difference),
          LocalDateTime.of(2018, 1, 1, 0, 0)
        )
        feeResponse.toPay(
          SubmissionReadyForReview,
          Some(SubmissionRequestStatus(true))
        ) mustEqual total
      }

      "status is NotCompleted" in {
        feeResponse.toPay(
          NotCompleted,
          Some(SubmissionRequestStatus(true))
        ) mustEqual total
      }

      "status is SubmissionReady" in {
        feeResponse.toPay(
          SubmissionReady,
          Some(SubmissionRequestStatus(true))
        ) mustEqual total
      }

      "status is SubmissionDecisionApproved" in {
        feeResponse.toPay(
          SubmissionDecisionApproved,
          Some(SubmissionRequestStatus(true))
        ) mustEqual total
      }

      "status is SubmissionDecisionRejected" in {
        feeResponse.toPay(
          SubmissionDecisionRejected,
          Some(SubmissionRequestStatus(true))
        ) mustEqual total
      }

      "status is SubmissionDecisionRevoked" in {
        feeResponse.toPay(
          SubmissionDecisionRevoked,
          Some(SubmissionRequestStatus(true))
        ) mustEqual total
      }

      "status is SubmissionDecisionExpired" in {
        feeResponse.toPay(
          SubmissionDecisionExpired,
          Some(SubmissionRequestStatus(true))
        ) mustEqual total
      }

      "status is SubmissionWithdrawn" in {
        feeResponse.toPay(
          SubmissionWithdrawn,
          Some(SubmissionRequestStatus(true))
        ) mustEqual total
      }

      "status is DeRegistered" in {
        feeResponse.toPay(
          DeRegistered,
          Some(SubmissionRequestStatus(true))
        ) mustEqual total
      }
    }
  }

  "ResponseType" must {

    "serialise SubscriptionResponseType to JSON" in {
      Json.toJson[ResponseType](SubscriptionResponseType) mustEqual JsString("SubscriptionReponse")
    }

    "serialise AmendOrVariationResponseType to JSON" in {
      Json.toJson[ResponseType](AmendOrVariationResponseType) mustEqual JsString("AmendOrVariationResponse")
    }

    "deserialise SubscriptionReponse from JSON" in {
      JsString("SubscriptionReponse").as[ResponseType] mustEqual SubscriptionResponseType
    }

    "deserialise AmendOrVariationResponse from JSON" in {
      JsString("AmendOrVariationResponse").as[ResponseType] mustEqual AmendOrVariationResponseType
    }

    "fail to deserialise an invalid ResponseType" in {
      JsString("invalid").validate[ResponseType] mustBe a[JsError]
    }
  }

  "FeeResponse JSON" must {

    val feeResponse = FeeResponse(
      AmendOrVariationResponseType,
      "XAML00000567890",
      BigDecimal(100),
      Some(BigDecimal(100)),
      None,
      BigDecimal(100),
      BigDecimal(100),
      None,
      Some(BigDecimal(50)),
      LocalDateTime.of(2018, 1, 1, 0, 0)
    )

    "serialise and deserialise via dateTimeWrite / dateTimeRead ($date long format)" in {
      val json   = Json.toJson(feeResponse)
      val result = json.as[FeeResponse]
      result mustEqual feeResponse
    }

    "deserialise from $date.$numberLong as Long format" in {
      val timestamp = 1514764800000L
      val json      = Json.obj(
        "responseType"        -> "AmendOrVariationResponse",
        "amlsReferenceNumber" -> "XAML00000567890",
        "registrationFee"     -> 100,
        "fpFee"               -> 100,
        "premiseFee"          -> 100,
        "totalFees"           -> 100,
        "difference"          -> 50,
        "createdAt"           -> Json.obj("$date" -> Json.obj("$numberLong" -> timestamp))
      )
      json.as[FeeResponse].createdAt mustEqual feeResponse.createdAt
    }

    "deserialise from $date.$numberLong as String format" in {
      val timestamp = "1514764800000"
      val json      = Json.obj(
        "responseType"        -> "AmendOrVariationResponse",
        "amlsReferenceNumber" -> "XAML00000567890",
        "registrationFee"     -> 100,
        "fpFee"               -> 100,
        "premiseFee"          -> 100,
        "totalFees"           -> 100,
        "difference"          -> 50,
        "createdAt"           -> Json.obj("$date" -> Json.obj("$numberLong" -> timestamp))
      )
      json.as[FeeResponse].createdAt mustEqual feeResponse.createdAt
    }

    "FeeResponse.toPay" must {

      "return difference when status is RenewalSubmitted and isRenewalAmendment is true" in {
        val fr = FeeResponse(
          AmendOrVariationResponseType,
          "XAML00000567890",
          BigDecimal(100),
          Some(BigDecimal(100)),
          None,
          BigDecimal(100),
          BigDecimal(100),
          None,
          Some(BigDecimal(50)),
          LocalDateTime.of(2018, 1, 1, 0, 0)
        )
        fr.toPay(
          RenewalSubmitted(None),
          Some(SubmissionRequestStatus(true, isRenewalAmendment = Some(true)))
        ) mustEqual BigDecimal(50)
      }
    }

  }
}
