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

import models.payments.{PaymentRedirectRequest, PaymentServiceRedirect}
import models.ReturnLocation
import org.apache.http.client.HttpResponseException
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Cookie, Cookies}
import play.api.test.FakeRequest
import play.api.test.Helpers._


import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{CorePost, HeaderCarrier, HttpResponse}

class PaymentsConnectorSpec extends PlaySpec with MockitoSugar with OneAppPerSuite {

  implicit val hc = HeaderCarrier()

  trait TestFixture {

    val http = mock[CorePost]
    val authConnector = mock[AuthConnector]

    val defaultBuilder = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .configure("microservice.services.feature-toggle.payments-url-lookup" -> true)
      .overrides(bind[AuthConnector].to(authConnector))
      .overrides(bind[CorePost].to(http))

    val builder = defaultBuilder
    lazy val app = builder.build()
    lazy val connector = app.injector.instanceOf[PaymentsConnector]

    val mdtpCookie = Cookie("mdtp", "hello")

    implicit val request = FakeRequest().withCookies(mdtpCookie)

    val returnLocation = ReturnLocation(controllers.routes.ConfirmationController.paymentConfirmation("reference").url)

    def createResponse(f: () => Future[HttpResponse]) = {
      when {
        http.POST[PaymentRedirectRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any())
      } thenReturn f()
    }

    when {
      authConnector.getCurrentAuthority(any(), any())
    } thenReturn Future.successful(mock[Authority])

    when {
      authConnector.getIds(any())(any(), any())
    } thenReturn Future.successful(Ids("an internal id"))
  }

  "The payments connector POST request" must {

    "Return the payments redirect url" when {

      "given valid payment details" in new TestFixture {

        val cookies = Seq(Cookie("mdtpp", "some_value"))

        createResponse { () =>
          Future.successful(HttpResponse(CREATED, responseHeaders = Map(
            "Location" -> Seq("http://localhost:9050/pay-online/card-selection"),
            "Set-Cookie" -> Seq(Cookies.encodeSetCookieHeader(cookies)))))
        }

        val model = PaymentRedirectRequest("reference_number", 150, returnLocation)

        val result = await(connector.requestPaymentRedirectUrl(model))

        result mustBe Some(PaymentServiceRedirect("http://localhost:9050/pay-online/card-selection", cookies))

        verify(http).POST(any(), any(), any())(any(), any(), any(), any())
      }
    }

    "returns None when the http request failed" in new TestFixture {

      createResponse { () =>
        Future.failed(new HttpResponseException(400, "The request failed"))
      }

      val model = PaymentRedirectRequest("reference_number", 150, returnLocation)

      val result = await(connector.requestPaymentRedirectUrl(model))

      result mustBe None
    }

    "not contact the payments api if the feature toggle is switched off" in new TestFixture {

      override val builder = defaultBuilder.configure("microservice.services.feature-toggle.payments-url-lookup" -> false)

      val model = PaymentRedirectRequest("reference_number", 150, returnLocation)

      val result = await(connector.requestPaymentRedirectUrl(model))

      result mustBe None
    }

  }

}
