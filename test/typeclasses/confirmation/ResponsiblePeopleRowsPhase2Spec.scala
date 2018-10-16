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
import models.responsiblepeople.ResponsiblePerson
import models.{SubscriptionFees, SubscriptionResponse}
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
    implicit val authContext = mock[AuthContext]
    implicit val headerCarrier = HeaderCarrier()

    "responsible people rows with phase2 toggle" when {
      "subscription with F&P and AP are used" in new Fixture {

        val paymentRefNo = "XA000000000000"
        val subscriptionResponse = SubscriptionResponse(
          etmpFormBundleNumber = "",
          amlsRefNo = amlsRegistrationNumber,
          Some(SubscriptionFees(
            registrationFee = 0,
            fpFee = None,
            fpFeeRate = None,
            approvalCheckFee = None,
            approvalCheckFeeRate = None,
            premiseFee = 0,
            premiseFeeRate = None,
            totalFees = 0,
            paymentReference = paymentRefNo
          )))

        val businessActivity = Set[BusinessActivity](models.businessmatching.MoneyServiceBusiness)
        val people: Option[Seq[ResponsiblePerson]] = None

        val result = ResponsiblePeopleRowsInstances.responsiblePeopleRowsFromSubscription(
          subscriptionResponse,
          activities = businessActivity,
          people)
      }
    }
  }
}
