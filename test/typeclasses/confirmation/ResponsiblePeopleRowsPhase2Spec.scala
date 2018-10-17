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

import connectors.DataCacheConnector
import generators.{AmlsReferenceNumberGenerator, ResponsiblePersonGenerator}
import models.businessmatching.BusinessActivity
import models.confirmation.{BreakdownRow, Currency}
import models.responsiblepeople.{ApprovalFlags, ResponsiblePerson}
import models.{AmendVariationRenewalResponse, SubscriptionFees, SubscriptionResponse}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import services.ConfirmationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext

class ResponsiblePeopleRowsPhase2Spec extends PlaySpec
  with MockitoSugar
  with ScalaFutures
  with IntegrationPatience
  with OneAppPerSuite
  with ResponsiblePersonGenerator
  with generators.tradingpremises.TradingPremisesGenerator
  with AmlsReferenceNumberGenerator {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.phase-2-changes" -> true)
    .build()

  trait Fixture {

    val TestConfirmationService = new ConfirmationService(
      mock[DataCacheConnector]
    ) 
  }
    implicit val authContext = mock[AuthContext]
    implicit val headerCarrier = HeaderCarrier()
    val subscriptionResponse = SubscriptionResponse(
      etmpFormBundleNumber = "",
      amlsRefNo = amlsRegistrationNumber,
      Some(SubscriptionFees(
        registrationFee = 0,
        fpFee = Some(100.00),
        fpFeeRate = None,
        approvalCheckFee = Some(200.00),
        approvalCheckFeeRate = Some(100.00),
        premiseFee = 0,
        premiseFeeRate = None,
        totalFees = 0,
        paymentReference = "XA000000000000"
      )))

    val amendVariationRenewalResponse = AmendVariationRenewalResponse(
      processingDate = "",
      etmpFormBundleNumber = "",
      registrationFee = 100,
      fpFee = None,
      fpFeeRate = None,
      approvalCheckFee = None,
      approvalCheckFeeRate = None,
      premiseFee = 0,
      premiseFeeRate = None,
      totalFees = 100,
      paymentReference = Some("XA000000000000"),
      difference = Some(0)
    )

    "responsible people rows with phase2 toggle" should {

      "return an approval check row" when {
        "The business is HVD, EAB or ASP and has answered no to both the approvals question and F&P question" in new Fixture {

          val businessActivity = Set[BusinessActivity](models.businessmatching.HighValueDealing)
          val people: Option[Seq[ResponsiblePerson]] = Some(
            Seq(
              ResponsiblePerson(
                approvalFlags = ApprovalFlags(
                  hasAlreadyPaidApprovalCheck = Some(false),
                  hasAlreadyPassedFitAndProper = Some(false)
                )
              )
            )
          )

          val result = ResponsiblePeopleRowsInstancesPhase2.responsiblePeopleRowsFromSubscription(
            subscriptionResponse,
            activities = businessActivity,
            people)

          val expectedResult = Seq(
            BreakdownRow(
              label = "confirmation.responsiblepeople.approvalcheck.notpassed",
              quantity = 1,
              perItm = Currency(100.00),
              total = Currency(200.00)
            )
          )
          result must be(expectedResult)
        }

        "The business is EAB and only one responsible person answered no to check approval question" in new Fixture {

          val businessActivity = Set[BusinessActivity](models.businessmatching.EstateAgentBusinessService)
          val people: Option[Seq[ResponsiblePerson]] = Some(
            Seq(
              ResponsiblePerson(
                approvalFlags = ApprovalFlags(
                  hasAlreadyPaidApprovalCheck = Some(true),
                  hasAlreadyPassedFitAndProper = None
                )
              ),
              ResponsiblePerson(
                approvalFlags = ApprovalFlags(
                  hasAlreadyPaidApprovalCheck = Some(false),
                  hasAlreadyPassedFitAndProper = None
                )
              )
            )
          )

          val result = ResponsiblePeopleRowsInstancesPhase2.responsiblePeopleRowsFromSubscription(
            subscriptionResponse,
            activities = businessActivity,
            people)

          val expectedResult = Seq(
            BreakdownRow(
              label = "confirmation.responsiblepeople.approvalcheck.notpassed",
              quantity = 1,
              perItm = Currency(100.00),
              total = Currency(200.00)
            )
          )
          result must be(expectedResult)
        }

        "The business is ASP and only two responsible persons answered no to check approval question" in new Fixture {

          val businessActivity = Set[BusinessActivity](models.businessmatching.AccountancyServices)
          val people: Option[Seq[ResponsiblePerson]] = Some(
            Seq(
              ResponsiblePerson(
                approvalFlags = ApprovalFlags(
                  hasAlreadyPaidApprovalCheck = Some(false),
                  hasAlreadyPassedFitAndProper = None
                )
              ),
              ResponsiblePerson(
                approvalFlags = ApprovalFlags(
                  hasAlreadyPaidApprovalCheck = Some(true),
                  hasAlreadyPassedFitAndProper = None
                )
              ),
              ResponsiblePerson(
                approvalFlags = ApprovalFlags(
                  hasAlreadyPaidApprovalCheck = Some(false),
                  hasAlreadyPassedFitAndProper = None
                )
              )
            )
          )

          val result = ResponsiblePeopleRowsInstancesPhase2.responsiblePeopleRowsFromSubscription(
            subscriptionResponse,
            activities = businessActivity,
            people)

          val expectedResult = Seq(
            BreakdownRow(
              label = "confirmation.responsiblepeople.approvalcheck.notpassed",
              quantity = 2,
              perItm = Currency(100.00),
              total = Currency(200.00)
            )
          )
          result must be(expectedResult)

        }
      }
      
      "not return an approval check row" when {
        "The business is MSB and HVD" in new Fixture {
          val businessActivity = Set[BusinessActivity](models.businessmatching.MoneyServiceBusiness, models.businessmatching.HighValueDealing)
          val people: Option[Seq[ResponsiblePerson]] = Some(
            Seq(
              ResponsiblePerson(
                approvalFlags = ApprovalFlags(
                  hasAlreadyPaidApprovalCheck = Some(true),
                  hasAlreadyPassedFitAndProper = Some(false)
                )
              )
            )
          )

          val result = ResponsiblePeopleRowsInstancesPhase2.responsiblePeopleRowsFromSubscription(
            subscriptionResponse,
            activities = businessActivity,
            people)

          val expectedResult = Seq.empty
          result must be(expectedResult)
        }

        "The business is MSB SubscriptionResponse" in new Fixture {
          val businessActivity = Set[BusinessActivity](models.businessmatching.MoneyServiceBusiness)
          val people: Option[Seq[ResponsiblePerson]] = Some(
            Seq(
              ResponsiblePerson(
                approvalFlags = ApprovalFlags(
                  hasAlreadyPaidApprovalCheck = Some(true),
                  hasAlreadyPassedFitAndProper = Some(false)
                )
              )
            )
          )

          val result = ResponsiblePeopleRowsInstancesPhase2.responsiblePeopleRowsFromSubscription(
            subscriptionResponse,
            activities = businessActivity,
            people)

          val expectedResult = Seq.empty
          result must be(expectedResult)
        }

        "The business is MSB for AmendVariationRenewalResponse" in new Fixture {
          val businessActivity = Set[BusinessActivity](models.businessmatching.MoneyServiceBusiness)
          val people: Option[Seq[ResponsiblePerson]] = Some(
            Seq(
              ResponsiblePerson(
                approvalFlags = ApprovalFlags(
                  hasAlreadyPaidApprovalCheck = Some(true),
                  hasAlreadyPassedFitAndProper = Some(true)
                )
              )
            )
          )

          val result = ResponsiblePeopleRowsInstancesPhase2.responsiblePeopleRowsFromVariation(
            amendVariationRenewalResponse,
            activities = businessActivity,
            people)

          val expectedResult = Seq.empty
          result must be(expectedResult)
        }

        "The business is TCSP" in new Fixture {
          val businessActivity = Set[BusinessActivity](models.businessmatching.TrustAndCompanyServices)
          val people: Option[Seq[ResponsiblePerson]] = Some(
            Seq(
              ResponsiblePerson(
                approvalFlags = ApprovalFlags(
                  hasAlreadyPaidApprovalCheck = Some(true),
                  hasAlreadyPassedFitAndProper = Some(false)
                )
              )
            )
          )

          val result = ResponsiblePeopleRowsInstancesPhase2.responsiblePeopleRowsFromSubscription(
            subscriptionResponse,
            activities = businessActivity,
            people)

          val expectedResult = Seq.empty
          result must be(expectedResult)
        }

        "The business has answered yes to Fit and Proper Question" in new Fixture {
          val businessActivity = Set[BusinessActivity](models.businessmatching.HighValueDealing)
          val people: Option[Seq[ResponsiblePerson]] = Some(
            Seq(
              ResponsiblePerson(
                approvalFlags = ApprovalFlags(
                  hasAlreadyPaidApprovalCheck = Some(true),
                  hasAlreadyPassedFitAndProper = Some(true)
                )
              )
            )
          )

          val result = ResponsiblePeopleRowsInstancesPhase2.responsiblePeopleRowsFromSubscription(
            subscriptionResponse,
            activities = businessActivity,
            people)

          val expectedResult = Seq.empty
          result must be(expectedResult)

        }

        "The business has answered yes to Approval Check Question" in new Fixture {
          val businessActivity = Set[BusinessActivity](models.businessmatching.HighValueDealing)
          val people: Option[Seq[ResponsiblePerson]] = Some(
            Seq(
              ResponsiblePerson(
                approvalFlags = ApprovalFlags(
                  hasAlreadyPaidApprovalCheck = Some(true),
                  hasAlreadyPassedFitAndProper = Some(false)
                )
              )
            )
          )

          val result = ResponsiblePeopleRowsInstancesPhase2.responsiblePeopleRowsFromSubscription(
            subscriptionResponse,
            activities = businessActivity,
            people)

          val expectedResult = Seq.empty
          result must be(expectedResult)

        }
      }

      "return a Fit and Proper row" when {
        "The business is MSB or TCSP along with HVD, EAB or ASP and hasn't passed F&P" in pending

        "The business is MSB or TCSP only and hasn't passed F&P" in pending
      }

      "Not return a Fit and Proper row" when {
        "The business has answered yes to Fit and Proper Question" in pending

        "The business doesn't have any business activities" in pending

        "The business is HVD, EAB or ASP and has answered no to both the approvals question and F&P question" in pending
      }
    }
}
