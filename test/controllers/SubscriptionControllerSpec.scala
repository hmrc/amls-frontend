package controllers

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.JsString
import play.api.test.Helpers._
import services.SubscriptionService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HttpResponse
import utils.AuthorisedFixture
import org.mockito.Matchers._
import org.mockito.Mockito._

import scala.concurrent.Future

class SubscriptionControllerSpec extends PlaySpec with OneServerPerSuite {

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new SubscriptionController {
      override private[controllers] val subscriptionService: SubscriptionService = mock[SubscriptionService]
      override protected def authConnector: AuthConnector = self.authConnector
    }
  }

  "SubscriptionController" must {

    "post must return the response from the service correctly" in new Fixture {
      val responseBody = Some(JsString("RESPONSE BODY"))
      when(controller.subscriptionService.subscribe(any(), any(), any())) thenReturn Future.successful(HttpResponse(OK, responseBody))
      val result = controller.post()(request)
      status(result) must be(OK)
      contentAsString(result) mustBe "\"RESPONSE BODY\""
    }

  }

}
