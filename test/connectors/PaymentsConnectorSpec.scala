package connectors

import models.payments.{PaymentRedirectRequest, PaymentServiceRedirect, ReturnLocation}
import org.apache.http.client.HttpResponseException
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsEmpty, Cookie, Cookies, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PaymentsConnectorSpec extends PlaySpec with MockitoSugar with OneAppPerSuite {

  implicit val hc = HeaderCarrier()

  trait TestFixture {

    val http = mock[HttpPost]
    val authConnector = mock[AuthConnector]

    val defaultBuilder = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .configure("Test.microservice.services.feature-toggle.payments-url-lookup" -> true)
      .overrides(bind[AuthConnector].to(authConnector))
      .overrides(bind[HttpPost].to(http))

    val builder = defaultBuilder
    lazy val app = builder.build()
    lazy val connector = app.injector.instanceOf[PaymentsConnector]

    val mdtpCookie = Cookie("mdtp", "hello")

    implicit val request = FakeRequest().withCookies(mdtpCookie)

    val returnLocation = ReturnLocation(controllers.routes.ConfirmationController.paymentConfirmation("reference").url)

    def createResponse(f: () => Future[HttpResponse]) = {
      when {
        http.POST[PaymentRedirectRequest, HttpResponse](any(), any(), any())(any(), any(), any())
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

        verify(http).POST(any(), any(), any())(any(), any(), any())
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

      override val builder = defaultBuilder.configure("Test.microservice.services.feature-toggle.payments-url-lookup" -> false)

      val model = PaymentRedirectRequest("reference_number", 150, returnLocation)

      val result = await(connector.requestPaymentRedirectUrl(model))

      result mustBe None
    }

  }

}
