/*
 * Copyright 2019 HM Revenue & Customs
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

import config.WSHttp
import models.ReturnLocation
import models.payments.{CreatePaymentRequest, CreatePaymentResponse, NextUrl}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent._
import play.api.inject.bind
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.AmlsSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PayApiConnectorSpec extends AmlsSpec with ScalaFutures with IntegrationPatience {

  implicit val request = FakeRequest("GET", "/anti-money-laundering/confirmation")

  trait TestFixture {
    val paymentAmount = 100

    val paymentId = "1234567890"
    val paymentUrl = "http://tax.service.gov.uk/pay/1234567890"

    val validRequest = CreatePaymentRequest(
      "other",
      "X12345678901234",
      "An example payment",
      paymentAmount,
      ReturnLocation("/confirmation", "http://localhost:9222"))

    val validResponse = CreatePaymentResponse(NextUrl(paymentUrl), paymentId)
    val http = mock[WSHttp]
    val payApiUrl = "http://localhost:9057"

    val auditConnector = mock[AuditConnector]

    val injector = new GuiceInjectorBuilder()
      .overrides(bind[WSHttp].to(http))
      .bindings(bind[AuditConnector].to(auditConnector))
      .build()

    lazy val connector = injector.instanceOf[PayApiConnector]
  }

  "The Pay-API connector" when {
    "the 'createPayment' method is called" when {
      "the payments feature is toggled on" must {
        "make a request to the payments API" in new TestFixture {
          when {
            http.POST[CreatePaymentRequest, HttpResponse](eqTo(s"$payApiUrl/pay-api/amls/journey/start"), any(), any())(any(), any(), any(), any())
          } thenReturn Future.successful(
            HttpResponse(OK, Some(Json.toJson(validResponse)))
          )

          val result = await(connector.createPayment(validRequest))

          result mustBe Some(validResponse)
          verify(auditConnector).sendExtendedEvent(any())(any(), any())
        }
      }

      "the API returns a 400 code" must {
        "return no result and log the failure" in new TestFixture {
          when {
            http.POST[CreatePaymentRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any())
          } thenReturn Future.successful(
            HttpResponse(BAD_REQUEST, None)
          )

          val result = await(connector.createPayment(validRequest))

          result must not be defined
          verify(auditConnector).sendExtendedEvent(any())(any(), any())
        }
      }
    }
  }
}