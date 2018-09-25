/*
 * Copyright 2018 HM Revenue & Customs
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

package typeclasses.confirmation

import models.{AmendVariationRenewalResponse, SubscriptionFees, SubscriptionResponse}
import models.businessmatching.{BusinessActivities, BusinessActivity, MoneyServiceBusiness}
import models.confirmation.{BreakdownRow, Currency}
import models.responsiblepeople.{PersonName, ResponsiblePerson}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.FakeApplication
import utils.AuthorisedFixture

// TODO: Implement
class BreakdownRowsSpec extends PlaySpec with OneAppPerSuite {

    override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.phase-2-changes" -> true))

    val amendVariationRenewalResponse = AmendVariationRenewalResponse(
        processingDate = "",
        etmpFormBundleNumber = "",
        registrationFee = 1.0,
        fpFee = Some(2.0),
        fpFeeRate = Some(3.0),
        premiseFee = 4.0,
        premiseFeeRate = Some(5.0),
        totalFees = 6.0,
        paymentReference = None,
        difference = Some(7.0),
        addedResponsiblePeople = 8,
        addedResponsiblePeopleFitAndProper = 9,
        addedFullYearTradingPremises = 10,
        halfYearlyTradingPremises = 11,
        zeroRatedTradingPremises = 12
    )

    val subscriptionFees = Some(SubscriptionFees(
        registrationFee = 1.0,
        fpFee = Some(2.0),
        fpFeeRate = Some(3.0),
        premiseFee = 4.0,
        premiseFeeRate = Some(5.0),
        totalFees = 6.0,
        paymentReference = ""
    ))

    val subscriptionFeesNoBreakdown = Some(subscriptionFees.get.copy(fpFee = None))

    val subscriptionResponse = SubscriptionResponse(
        etmpFormBundleNumber = "",
        amlsRefNo = "",
        subscriptionFees = subscriptionFees,
        previouslySubmitted = None
    )

    val subscriptionResponseNoBreakdown = subscriptionResponse.copy(subscriptionFees = subscriptionFeesNoBreakdown)

    val activities: Set[BusinessActivity] = Set(
        MoneyServiceBusiness
    )

    val responsiblePeople = Some(Seq(
        ResponsiblePerson(personName = Some(PersonName("firstName", None, "lastName")))
    ))

    val premises = Some(Seq(
        // TODO: Populate with premises
    ))

  trait BreakdownRowsFixture extends AuthorisedFixture {

    val breakdownRowsAmendVariationRenewalShowBreakdown: Seq[BreakdownRow] = BreakdownRowInstances.
      breakdownRowFromVariation(
        amendVariationRenewalResponse,
        Some(BusinessActivities(activities)),
        premises,
        responsiblePeople
      )

    val breakdownRowsSubscriptionShowBreakdown: Seq[BreakdownRow] = BreakdownRowInstances.
      breakdownRowFromSubscription(
        subscriptionResponse,
        Some(BusinessActivities(activities)),
        premises,
        responsiblePeople
      )
  }

  "value is a AmendVariationRenewalResponse" when {

      "set BreakdownRows for responsible people" in new BreakdownRowsFixture {
        breakdownRowsAmendVariationRenewalShowBreakdown.filter(
          _.label == "confirmation.responsiblepeople"
        ) mustEqual Seq(
          BreakdownRow("confirmation.responsiblepeople", 8, Currency(3), Currency(2))
        )
      }

      "set BreakdownRows for fit & proper charge" in new BreakdownRowsFixture {
        breakdownRowsAmendVariationRenewalShowBreakdown.filter(
          _.label == "confirmation.responsiblepeople.fp.passed"
        ) mustEqual Seq(
          BreakdownRow("confirmation.responsiblepeople.fp.passed", 9, Currency(0.00), Currency(0.00))
        )
      }

      "set BreakdownRows for TradingPremises zero" in new BreakdownRowsFixture {
        breakdownRowsAmendVariationRenewalShowBreakdown.filter(
          _.label == "confirmation.tradingpremises.zero"
        ) mustEqual Seq(
          BreakdownRow("confirmation.tradingpremises.zero", 12, Currency(0.00), Currency(0.00))
        )
      }

      "set BreakdownRows for TradingPremises half" in new BreakdownRowsFixture {
        breakdownRowsAmendVariationRenewalShowBreakdown.filter(
          _.label == "confirmation.tradingpremises.half"
        ) mustEqual Seq(
          BreakdownRow("confirmation.tradingpremises.half", 11,Currency(2.50),Currency(27.50))
        )
      }

      "set BreakdownRows for TradingPremises full" in new BreakdownRowsFixture {
        breakdownRowsAmendVariationRenewalShowBreakdown.filter(
          _.label == "confirmation.tradingpremises"
        ) mustEqual Seq(
          BreakdownRow("confirmation.tradingpremises",10,Currency(5.00),Currency(50.00))
        )
      }
    }

  "value is a SubmissionResponse" when {

      "set BreakdownRows for responsible people" in new BreakdownRowsFixture {
        breakdownRowsSubscriptionShowBreakdown.filter(
          _.label == "confirmation.responsiblepeople"
        ) mustEqual Seq(
          BreakdownRow("confirmation.responsiblepeople",1,Currency(3.00),Currency(2.00))
        )
      }

      "set BreakdownRows for fit & proper charge" in new BreakdownRowsFixture {
        breakdownRowsSubscriptionShowBreakdown.filter(
          _.label == "confirmation.responsiblepeople.fp.passed"
        ) mustEqual Seq.empty
      }

      "set BreakdownRows for TradingPremises zero" in new BreakdownRowsFixture {
        breakdownRowsSubscriptionShowBreakdown.filter(
          _.label == "confirmation.tradingpremises.zero"
        ) mustEqual Seq.empty
      }

      "set BreakdownRows for TradingPremises half" in new BreakdownRowsFixture {
        breakdownRowsSubscriptionShowBreakdown.filter(
          _.label == "confirmation.tradingpremises.half"
        ) mustEqual Seq.empty
      }

      "set BreakdownRows for TradingPremises full" in new BreakdownRowsFixture {
        breakdownRowsSubscriptionShowBreakdown.filter(
          _.label == "confirmation.tradingpremises"
        ) mustEqual Seq(
          BreakdownRow("confirmation.tradingpremises",0,Currency(5.00),Currency(4.00))
        )
      }
    }
}
