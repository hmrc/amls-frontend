package controllers

import models.SubscriptionResponse
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import services.SubscriptionService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture

import scala.concurrent.Future

class SubscriptionControllerSpec extends PlaySpec with OneServerPerSuite {

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new SubscriptionController {
      override private[controllers] val subscriptionService: SubscriptionService = mock[SubscriptionService]
      override protected val authConnector: AuthConnector = self.authConnector
    }
  }

  "SubscriptionController" must {

    val response = SubscriptionResponse(
      etmpFormBundleNumber = "",
      amlsRefNo = "",
      registrationFee = 0,
      fpFee = None,
      premiseFee = 0,
      totalFees = 0,
      paymentReference = ""
    )

    "post must return the response from the service correctly" in new Fixture {
      when(controller.subscriptionService.subscribe(any(), any(), any())) thenReturn Future.successful(response)
      val result = controller.post()(request)
      status(result) must be(OK)
      contentAsString(result) mustBe Json.toJson(response).toString
    }
  }
}
