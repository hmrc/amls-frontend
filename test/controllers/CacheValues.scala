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

package controllers

import models.amp.Amp
import models.asp.Asp
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businessdetails.BusinessDetails
import models.businessmatching._
import models.eab.Eab
import models.hvd.Hvd
import models.moneyservicebusiness.MoneyServiceBusiness
import models.renewal.Renewal
import models.responsiblepeople._
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import models.{status => _, _}
import play.api.libs.json.{JsValue, Json}
import services.cache.Cache

trait CacheValues {

  def createEmptyTestCache(): Cache =
    Cache("empty-test-cache", Map.empty[String, JsValue])

  def createTestCache(
    hasChanged: Boolean,
    includesResponse: Boolean,
    noTP: Boolean = false,
    noRP: Boolean = false,
    includeSubmissionStatus: Boolean = false,
    includeDataImport: Boolean = false
  ): Cache = {

    val data: Map[String, JsValue] = Map(
      Asp.key                  -> Json.toJson(Asp(hasChanged = hasChanged)),
      Amp.key                  -> Json.toJson(Amp(hasChanged = hasChanged)),
      BusinessDetails.key      -> Json.toJson(BusinessDetails(hasChanged = hasChanged)),
      BankDetails.key          -> Json.toJson(Seq(BankDetails(hasChanged = hasChanged))),
      BusinessActivities.key   -> Json.toJson(BusinessActivities(hasChanged = hasChanged)),
      BusinessMatching.key     -> Json.toJson(BusinessMatching(hasChanged = hasChanged)),
      Eab.key                  -> Json.toJson(Eab(hasChanged = hasChanged)),
      MoneyServiceBusiness.key -> Json.toJson(MoneyServiceBusiness(hasChanged = hasChanged)),
      Supervision.key          -> Json.toJson(Supervision(hasChanged = hasChanged)),
      Tcsp.key                 -> Json.toJson(Tcsp(hasChanged = hasChanged)),
      Hvd.key                  -> Json.toJson(Hvd(hasChanged = hasChanged)),
      Renewal.key              -> Json.toJson(Renewal(hasChanged = hasChanged))
    )

    val mapWithDataImport =
      if (includeDataImport) data + (DataImport.key -> Json.toJson(DataImport("test.json"))) else data

    val mapWithSubmissionStatus =
      if (includeSubmissionStatus)
        mapWithDataImport + (SubmissionRequestStatus.key -> Json.toJson(SubmissionRequestStatus(true)))
      else mapWithDataImport

    val mapWithTradingPremises =
      if (!noTP)
        mapWithSubmissionStatus + (TradingPremises.key -> Json.toJson(Seq(TradingPremises(hasChanged = hasChanged))))
      else mapWithSubmissionStatus

    val mapWithResponsiblePeople =
      if (!noRP)
        mapWithTradingPremises + (ResponsiblePerson.key -> Json.toJson(Seq(ResponsiblePerson(hasChanged = hasChanged))))
      else mapWithTradingPremises

    val mapWithSubscriptionResponse = if (includesResponse) {
      val testResponse = SubscriptionResponse(
        etmpFormBundleNumber = "TESTFORMBUNDLENUMBER",
        amlsRefNo = "TESTAMLSREFNNO",
        subscriptionFees = Some(
          SubscriptionFees(
            paymentReference = "TESTPAYMENTREF",
            registrationFee = 100.45,
            fpFee = None,
            fpFeeRate = None,
            approvalCheckFee = None,
            approvalCheckFeeRate = None,
            premiseFee = 123.78,
            premiseFeeRate = None,
            totalFees = 17623.76
          )
        )
      )

      mapWithResponsiblePeople + (SubscriptionResponse.key -> Json.toJson(testResponse))
    } else {
      mapWithResponsiblePeople
    }

    Cache("test-cache", mapWithSubscriptionResponse)
  }
}
