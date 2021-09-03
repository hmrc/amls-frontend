/*
 * Copyright 2021 HM Revenue & Customs
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

package services

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
import connectors.DataCacheConnector
import generators.{AmlsReferenceNumberGenerator, ResponsiblePersonGenerator}
import models._
import models.businesscustomer.ReviewDetails
import models.businessmatching._
import models.responsiblepeople.ResponsiblePerson
import models.tradingpremises.TradingPremises
import org.joda.time.DateTime
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

class ConfirmationServiceSpec extends PlaySpec
  with MockitoSugar
  with ScalaFutures
  with IntegrationPatience
  with GuiceOneAppPerSuite
  with ResponsiblePersonGenerator
  with generators.tradingpremises.TradingPremisesGenerator
  with AmlsReferenceNumberGenerator {

  trait Fixture {

    val TestConfirmationService = new ConfirmationService (
      mock[DataCacheConnector]
    )

    val rpFee: BigDecimal = 100
    val rpFeeWithRate: BigDecimal = 130
    val tpFee: BigDecimal = 115
    val tpFeeWithRate: BigDecimal = 125
    val tpHalfFee: BigDecimal = tpFee / 2
    val tpTotalFee: BigDecimal = tpFee + (tpHalfFee * 3)
    val totalFee: BigDecimal = rpFee + tpTotalFee

    val paymentRefNo = "XA000000000000"
    val credId = "credId"

    implicit val headerCarrier = HeaderCarrier()

    val subscriptionResponse = SubscriptionResponse(
      etmpFormBundleNumber = "",
      amlsRefNo = amlsRegistrationNumber,
      Some(SubscriptionFees(
        registrationFee = 0,
        fpFee = None,
        fpFeeRate = Some(100),
        approvalCheckFee = None,
        approvalCheckFeeRate = Some(40),
        premiseFee = 0,
        premiseFeeRate = Some(115),
        totalFees = 0,
        paymentReference = paymentRefNo
      )))

    val amendmentResponse = AmendVariationRenewalResponse(
      processingDate = "",
      etmpFormBundleNumber = "",
      registrationFee = 100,
      fpFee = None,
      fpFeeRate = Some(100),
      approvalCheckFee = None,
      approvalCheckFeeRate = Some(40),
      premiseFee = 0,
      premiseFeeRate = Some(115),
      totalFees = 100,
      paymentReference = Some(paymentRefNo),
      difference = Some(0)
    )

    val variationResponse = AmendVariationRenewalResponse(
      processingDate = "",
      etmpFormBundleNumber = "",
      registrationFee = 100,
      fpFee = None,
      fpFeeRate = Some(100),
      approvalCheckFee = None,
      approvalCheckFeeRate = Some(40),
      premiseFee = 0,
      premiseFeeRate = Some(115),
      totalFees = 100,
      paymentReference = Some(""),
      difference = Some(0)
    )

    def feeResponse(responseType: ResponseType) = FeeResponse(
      responseType,
      amlsRegistrationNumber,
      100,
      None,
      None,
      0,
      100,
      Some(paymentRefNo),
      None,
      DateTime.now
    )

    val reviewDetails = mock[ReviewDetails]
    val activities = mock[BusinessActivities]
    val businessMatching = mock[BusinessMatching]
    val cache = mock[CacheMap]

    when {
      businessMatching.activities
    } thenReturn Some(activities)

    when {
      activities.businessActivities
    } thenReturn Set[BusinessActivity]()

    when {
      cache.getEntry[BusinessMatching](BusinessMatching.key)
    } thenReturn Some(businessMatching)

    when {
      cache.getEntry[AmendVariationRenewalResponse](AmendVariationRenewalResponse.key)
    } thenReturn Some(amendmentResponse)

    when {
      cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
    } thenReturn Some(Seq(tradingPremisesGen.sample.get))

    when {
      cache.getEntry[Seq[ResponsiblePerson]](eqTo(ResponsiblePerson.key))(any())
    } thenReturn Some(Seq(ResponsiblePerson()))

    when {
      TestConfirmationService.cacheConnector.fetchAll(eqTo(credId))(any())
    } thenReturn Future.successful(Some(cache))
  }

  "SubmissionResponseService" when {

  }
}
