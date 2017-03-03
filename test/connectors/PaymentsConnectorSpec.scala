package connectors

import models.payments.{PaymentRedirectRequest, PaymentServiceRedirect}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PaymentsConnectorSpec extends PlaySpec with MockitoSugar {

  implicit val hc = HeaderCarrier()

  trait TestFixture {

    val http = mock[WSClient]
    val request = mock[WSRequest]

    when(http.url(any())) thenReturn request
    when(request.withFollowRedirects(any())) thenReturn request
    when(request.withHeaders(any())) thenReturn request

    val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[WSClient].to(http))
      .build()

    lazy val connector = app.injector.instanceOf[PaymentsConnector]

    def createResponse(status: Int, locationHeader: Option[String] = None) = {
      val response = mock[WSResponse]

      when(response.header("Location")) thenReturn locationHeader
      when(response.status) thenReturn status

      response
    }

  }

  "The payments connector" must {

    "Return the payments redirect url" when {

      "given valid payment details" in new TestFixture {

        val response = createResponse(SEE_OTHER, Some("/pay-online/card-selection"))

        when {
          request.post[String](any())(any())
        } thenReturn Future.successful(response)

        val model = PaymentRedirectRequest("reference_number", 150, "http://google.co.uk")

        val result = await(connector.requestPaymentRedirectUrl(model))

        result mustBe Some(PaymentServiceRedirect("http://localhost:9050/pay-online/card-selection"))

      }

    }

  }

}
