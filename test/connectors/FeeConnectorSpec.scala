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

package connectors

import config.ApplicationConfig
import generators.AmlsReferenceNumberGenerator
import models.ResponseType.SubscriptionResponseType
import models._
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FeeConnectorSpec extends PlaySpec with MockitoSugar with ScalaFutures with IntegrationPatience with AmlsReferenceNumberGenerator {

  trait Fixture {
    val connector = new FeeConnector(
      http = mock[HttpClient],
      appConfig = mock[ApplicationConfig])

    val safeId = "SAFEID"
    val accountTypeId = ("org", "id")

    implicit val hc = HeaderCarrier()

    when {
      connector.feePaymentUrl
    } thenReturn "/amls/feePaymentUrl"
  }


  "FeeConnector" must {

    val feeResponse = FeeResponse(
      responseType = SubscriptionResponseType,
      amlsReferenceNumber = amlsRegistrationNumber,
      registrationFee = 150.00,
      fpFee = Some(100.0),
      approvalCheckFee = None,
      premiseFee = 300.0,
      totalFees = 550.0,
      paymentReference = Some("XA000000000000"),
      difference = None,
      createdAt = new DateTime(2017, 12, 1, 1, 3, DateTimeZone.UTC))

    "successfully receive feeResponse" in new Fixture {

      when {
        connector.http.GET[FeeResponse](any(), any(), any())(any(),any(), any())
      } thenReturn Future.successful(feeResponse)

      whenReady(connector.feeResponse(amlsRegistrationNumber, accountTypeId)){
        _ mustBe feeResponse
      }
    }
  }
}
