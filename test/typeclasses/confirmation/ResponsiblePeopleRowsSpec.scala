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

import models.businessactivities.BusinessActivities
import models.{AmendVariationRenewalResponse, SubmissionResponse, SubscriptionFees, SubscriptionResponse}
import models.businessmatching.{BusinessActivity, MoneyServiceBusiness}
import models.confirmation.BreakdownRow
import models.responsiblepeople.{PersonName, ResponsiblePerson}
import org.scalatestplus.play.PlaySpec

// TODO: Implement
class ResponsiblePeopleRowsSpec extends PlaySpec {

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

    val subscriptionResponse = SubscriptionResponse(
      etmpFormBundleNumber = "",
      amlsRefNo = "",
      subscriptionFees = Some(SubscriptionFees(
        registrationFee = 1.0,
        fpFee = Some(2.0),
        fpFeeRate = Some(3.0),
        premiseFee = 4.0,
        premiseFeeRate = Some(5.0),
        totalFees = 6.0,
        paymentReference = ""
      )),
      previouslySubmitted = None
    )

    val activities: Set[BusinessActivity] = Set(
        MoneyServiceBusiness
    )

    val responsiblePeople = Some(Seq(
        ResponsiblePerson(personName = Some(PersonName("firstName", None, "lastName")))
    ))

    "value is a AmendVariationRenewalResponse" when {
        "businessActivities is None" must {
            "set BreakdownRows for responsible people" in {
              val breakdownRows: Seq[BreakdownRow] = ResponsiblePeopleRowsInstances.
                responsiblePeopleRowsFromVariation(
                  amendVariationRenewalResponse,
                  activities,
                  responsiblePeople
                )
            }

            "set BreakdownRows for fit & proper charge" in {

            }
        }

        "businessActivities is not None" must {
            "set BreakdownRows for responsible people" in {

            }

            "set BreakdownRows for fit & proper charge" in {

            }
        }
    }

    "value is a SubscriptionResponse" when {
        "businessActivities is None" must {
            "set BreakdownRows for responsible people" in {

            }

            "set BreakdownRows for fit & proper charge" in {

            }
        }

        "businessActivities is not None" must {
            "set BreakdownRows for responsible people" in {

            }

            "set BreakdownRows for fit & proper charge" in {

            }
        }
    }

}
