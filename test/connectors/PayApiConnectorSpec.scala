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

import models.ReturnLocation
import models.payments.{CreatePaymentRequest, CreatePaymentResponse, NextUrl}
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent._
import play.api.libs.json.{JsNull, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.play.audit.DefaultAuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.AmlsSpec

import scala.concurrent.Future

class PayApiConnectorSpec extends AmlsSpec with IntegrationPatience  {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/anti-money-laundering/confirmation")

  trait TestFixture {

    val paymentAmount = 100

    val paymentId = "1234567890"
    val paymentUrl = "http://tax.service.gov.uk/pay/1234567890"

    val validRequest: CreatePaymentRequest = CreatePaymentRequest(
      "other",
      "X12345678901234",
      "An example payment",
      paymentAmount,
      ReturnLocation("/confirmation", "http://localhost:9222"))

    val validResponse: CreatePaymentResponse = CreatePaymentResponse(NextUrl(paymentUrl), paymentId)
    val http: HttpClient = mock[HttpClient]
    val payApiUrl = "http://localhost:9057"

    val auditConnector: AuditConnector = mock[AuditConnector]

    val connector = new PayApiConnector(http, mock[DefaultAuditConnector], appConfig)

  }

  "The Pay-API connector" when {
    "the 'createPayment' method is called" when {
      "the payments feature is toggled on" must {
        "make a request to the payments API" in new TestFixture {
          implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

          when {
            http.POST[CreatePaymentRequest, HttpResponse](eqTo(s"$payApiUrl/pay-api/amls/journey/start"), any(), any())(any(), any(), any(), any())
          } thenReturn Future.successful(
            HttpResponse(OK, Json.toJson(validResponse), Map.empty[String, Seq[String]])
          )

          val result: Option[CreatePaymentResponse] = await(connector.createPayment(validRequest))

          result mustBe Some(validResponse)
          verify(connector.auditConnector).sendExtendedEvent(any())(any(), any())
        }
      }

      "the API returns a 400 code" must {
        "return no result and log the failure" in new TestFixture {
          implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

          when {
            http.POST[CreatePaymentRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any())
          } thenReturn Future.successful(
            HttpResponse(BAD_REQUEST, JsNull, Map.empty[String, Seq[String]])
          )

          val result: Option[CreatePaymentResponse] = await(connector.createPayment(validRequest))

          result must not be defined
          verify(connector.auditConnector).sendExtendedEvent(any())(any(), any())
        }
      }
    }
  }
}