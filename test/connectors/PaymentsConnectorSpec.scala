package connectors

import models.payments.{PaymentRedirectRequest, PaymentServiceRedirect}
import org.apache.http.client.HttpResponseException
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PaymentsConnectorSpec extends PlaySpec with MockitoSugar {

  implicit val hc = HeaderCarrier()

  trait TestFixture {

    val http = mock[HttpPost]

    val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[HttpPost].to(http))
      .build()

    lazy val connector = app.injector.instanceOf[PaymentsConnector]

    implicit val request = FakeRequest()

    def createResponse(f: () => Future[HttpResponse]) = {
      when {
        http.POST[PaymentRedirectRequest, HttpResponse](any(), any(), any())(any(), any(), any())
      } thenReturn f()
    }

  }

  "The payments connector" must {

    "Return the payments redirect url" when {

      "given valid payment details" in new TestFixture {

        createResponse { () =>
          Future.successful(HttpResponse(CREATED, responseHeaders = Map("Location" -> Seq("http://localhost:9050/pay-online/card-selection"))))
        }

        val model = PaymentRedirectRequest("reference_number", 150, "http://google.co.uk")

        val result = await(connector.requestPaymentRedirectUrl(model))

        result mustBe Some(PaymentServiceRedirect("http://localhost:9050/pay-online/card-selection"))

        verify(http).POST(any(), any(), any())(any(), any(), any())

      }
    }

    "returns None when the http request failed" in new TestFixture {

      createResponse { () =>
        Future.failed(new HttpResponseException(400, "The request failed"))
      }

      val model = PaymentRedirectRequest("reference_number", 150, "http://google.co.uk")

      val result = await(connector.requestPaymentRedirectUrl(model))

      result mustBe None
    }

  }

}
