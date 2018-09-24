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
import models.businessmatching.{BusinessActivity, MoneyServiceBusiness}
import models.responsiblepeople.{PersonName, ResponsiblePerson}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.FakeApplication

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

    // TODO: lets just make this phase 2 dependent given its coming in for that reason, discuss?
    "value is a AmendVariationRenewalResponse" when {
        "businessActivities contains MSB" must {
            "set BreakdownRows for responsible people" in {

            }

            "set BreakdownRows for fit & proper charge" in {

            }
        }

        "businessActivities contains TCSP" must {
            "set BreakdownRows for responsible people" in {

            }

            "set BreakdownRows for fit & proper charge" in {

            }
        }

        "businessActivities contains neither MSB or TCSP" must {
            "set BreakdownRows for responsible people" in {

            }

            "set BreakdownRows for fit & proper charge" in {

            }
        }
    }

    "value is a SubmissionResponse" when {
        "businessActivities contains MSB" must {
            "set BreakdownRows for responsible people" in {

            }

            "set BreakdownRows for fit & proper charge" in {

            }
        }

        "businessActivities contains TCSP" must {
            "set BreakdownRows for responsible people" in {

            }

            "set BreakdownRows for fit & proper charge" in {

            }
        }

        "businessActivities contains neither MSB or TCSP" must {
            "set BreakdownRows for responsible people" in {

            }

            "set BreakdownRows for fit & proper charge" in {

            }
        }
    }

}
