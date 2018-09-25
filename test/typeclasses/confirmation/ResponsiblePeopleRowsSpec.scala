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

import models.businessmatching.{BusinessActivity, MoneyServiceBusiness}
import models.confirmation.BreakdownRow
import models.responsiblepeople.{PersonName, ResponsiblePerson}
import models.{AmendVariationRenewalResponse, SubscriptionFees, SubscriptionResponse}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.FakeApplication
import utils.AuthorisedFixture

// TODO: These tests are commented out for now due to uncertainity about whether this typeclass exhibits correct behaviour
// TODO: Aim to uncomment the tests and verify behaviour when approval check is added to Fee Breakdown table
class ResponsiblePeopleRowsSpec extends PlaySpec with OneAppPerSuite {

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
        ResponsiblePerson(personName = Some(PersonName("firstName", None, "lastName"))),
        ResponsiblePerson(personName = Some(PersonName("firstName", None, "lastName")), hasAlreadyPassedFitAndProper = Some(true))
    ))

    trait ResponsiblePeopleRowsFixture extends AuthorisedFixture {
        val breakdownRowsAmendVariationRenewalShowBreakdown: Seq[BreakdownRow] = ResponsiblePeopleRowsInstances.
                responsiblePeopleRowsFromVariation(
                    amendVariationRenewalResponse,
                    activities,
                    responsiblePeople
                )

        val breakdownRowsAmendVariationRenewalNotShowBreakdown: Seq[BreakdownRow] = ResponsiblePeopleRowsInstances.
                responsiblePeopleRowsFromVariation(
                    amendVariationRenewalResponse.copy(fpFee = None),
                    activities,
                    responsiblePeople
                )

        val breakdownRowsSubscriptionShowBreakdown: Seq[BreakdownRow] = ResponsiblePeopleRowsInstances.
                responsiblePeopleRowsFromSubscription(
                    subscriptionResponse,
                    activities,
                    responsiblePeople
                )

        val breakdownRowsSubscriptionNotShowBreakdown: Seq[BreakdownRow] = ResponsiblePeopleRowsInstances.
                responsiblePeopleRowsFromSubscription(
                    subscriptionResponseNoBreakdown,
                    activities,
                    responsiblePeople
                )
    }

//    "value is a AmendVariationRenewalResponse" when {
//        "show breakdown" must {
//            "set BreakdownRows for responsible people" in new ResponsiblePeopleRowsFixture {
//                breakdownRowsAmendVariationRenewalShowBreakdown.filter(
//                    _.label == "confirmation.responsiblepeople.fp.passed"
//                ) mustEqual Seq(
//                    BreakdownRow("confirmation.responsiblepeople.fp.passed", 9, Currency(0), Currency(0))
//                )
//            }
//
//            "set BreakdownRows for fit & proper charge" in new ResponsiblePeopleRowsFixture {
//                breakdownRowsAmendVariationRenewalShowBreakdown.filter(
//                    _.label == "confirmation.responsiblepeople"
//                ) mustEqual Seq(
//                    BreakdownRow("confirmation.responsiblepeople", 8, Currency(3), Currency(2))
//                )
//            }
//        }
//
//        "not show breakdown" must {
//            "set BreakdownRows for responsible people" in new ResponsiblePeopleRowsFixture {
//                breakdownRowsAmendVariationRenewalNotShowBreakdown.filter(
//                    _.label == "confirmation.responsiblepeople.fp.passed"
//                ) mustEqual Seq.empty
//            }
//
//            "set BreakdownRows for fit & proper charge" in new ResponsiblePeopleRowsFixture {
//                breakdownRowsAmendVariationRenewalNotShowBreakdown.filter(
//                    _.label == "confirmation.responsiblepeople"
//                ) mustEqual Seq.empty
//            }
//        }
//    }
//
//    "value is a SubscriptionResponse" when {
//        "show breakdown" must {
//            "set BreakdownRows for responsible people" in new ResponsiblePeopleRowsFixture {
//                breakdownRowsSubscriptionShowBreakdown.filter(
//                    _.label == "confirmation.responsiblepeople.fp.passed"
//                ) mustEqual Seq(
//                    BreakdownRow("confirmation.responsiblepeople.fp.passed", 1, Currency(0), Currency(0))
//                )
//            }
//
//            "set BreakdownRows for fit & proper charge" in new ResponsiblePeopleRowsFixture {
//                breakdownRowsSubscriptionShowBreakdown.filter(
//                    _.label == "confirmation.responsiblepeople"
//                ) mustEqual Seq(
//                    BreakdownRow("confirmation.responsiblepeople", 1, Currency(3), Currency(2))
//                )
//            }
//        }
//
//        "not show breakdown" must {
//            "set BreakdownRows for responsible people" in new ResponsiblePeopleRowsFixture {
//                breakdownRowsSubscriptionNotShowBreakdown.filter(
//                    _.label == "confirmation.responsiblepeople.fp.passed"
//                ) mustEqual Seq.empty
//            }
//
//            "set BreakdownRows for fit & proper charge" in new ResponsiblePeopleRowsFixture {
//                breakdownRowsSubscriptionNotShowBreakdown.filter(
//                    _.label == "confirmation.responsiblepeople"
//                ) mustEqual Seq.empty
//            }
//        }
//    }

}
