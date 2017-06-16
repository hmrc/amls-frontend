/*
 * Copyright 2017 HM Revenue & Customs
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

import models.payments.{CreatePaymentResponse, PayApiRequest}
import org.scalatest.MustMatchers
import org.scalatest.concurrent._
import org.scalatestplus.play.PlaySpec
import play.api.inject.guice.GuiceInjectorBuilder
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => eqTo, _}
import org.scalatest.mock.MockitoSugar
import play.api.inject.bind
import play.api.http.Status._
import cats.implicits._
import config.ApplicationConfig
import uk.gov.hmrc.play.config.inject.ServicesConfig

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class PayApiConnectorSpec extends PlaySpec with MustMatchers with ScalaFutures with MockitoSugar {

  implicit val headerCarrier = HeaderCarrier()

  trait TestFixture {
    val paymentAmount = 100

    val paymentId = "763843249809843"

    val validRequest = PayApiRequest(
      "other",
      "X12345678901234",
      "An example payment",
      paymentAmount,
      "http://localhost:9222/anti-money-laundering")

    val validResponse = CreatePaymentResponse(paymentId)
    val paymentsToggleValue = true
    val httpPost = mock[HttpPost]

    val config = new ServicesConfig {
      override protected def environment = mock[play.api.Environment]
      override def getConfBool(confKey: String, defBool: => Boolean) = confKey match {
        case ApplicationConfig.paymentsUrlLookupToggleName => paymentsToggleValue
        case _ => super.getConfBool(confKey, defBool)
      }
    }

    val injector = new GuiceInjectorBuilder()
      .overrides(bind[HttpPost].to(httpPost))
      .bindings(bind[ServicesConfig].to(config))
      .build()

    lazy val connector = injector.instanceOf[PayApiConnector]
  }

  "The Pay-API connector" when {
    "the 'createPayment' method is called" when {
      "the payments feature is toggled on" must {
        "make a request to the payments API" in new TestFixture {
          when {
            httpPost.POST[PayApiRequest, CreatePaymentResponse](any(), any(), any())(any(), any(), any())
          } thenReturn Future.successful(validResponse)

          whenReady(connector.createPayment(validRequest)) {
            case Some(response) => response.id mustBe paymentId
          }
        }
      }

      "the payments feature is toggled off" must {
        "return no result" in new TestFixture {

          override val paymentsToggleValue = false

          whenReady(connector.createPayment(validRequest)) { r =>
            r must not be defined
          }
        }
      }
    }
  }
}
