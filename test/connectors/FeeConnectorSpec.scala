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

package connectors

import config.ApplicationConfig
import generators.AmlsReferenceNumberGenerator
import models.ResponseType.SubscriptionResponseType
import models._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.HttpClientMocker

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global

class FeeConnectorSpec extends PlaySpec with MockitoSugar with ScalaFutures with IntegrationPatience with AmlsReferenceNumberGenerator {

  trait Fixture {

    val mocker = new HttpClientMocker()
    private val configuration: Configuration = Configuration.load(Environment.simple())
    private val config = new ApplicationConfig(configuration, new ServicesConfig(configuration))

    val connector = new FeeConnector(
      http = mocker.httpClient,
      appConfig =config)

    val safeId = "SAFEID"
    val accountTypeId: (String, String) = ("org", "id")

    implicit val hc: HeaderCarrier = HeaderCarrier()
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
      createdAt = LocalDateTime.of(2017, 12, 1, 1, 3))

    "successfully receive feeResponse" in new Fixture {

      mocker.mockGet(url"http://localhost:8940/amls/payment/org/id/$amlsRegistrationNumber", feeResponse)

      whenReady(connector.feeResponse(amlsRegistrationNumber, accountTypeId)){
        _ mustBe feeResponse
      }
    }
  }
}
