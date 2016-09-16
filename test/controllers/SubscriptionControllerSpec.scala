package controllers

import models.SubscriptionResponse
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.JsString
import play.api.test.Helpers._
import services.SubmissionService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HttpResponse
import utils.AuthorisedFixture
import org.mockito.Matchers._
import org.mockito.Mockito._

import scala.concurrent.Future

class SubscriptionControllerSpec extends PlaySpec with OneAppPerSuite {

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new SubscriptionController {
      override private[controllers] val subscriptionService: SubmissionService = mock[SubmissionService]
      override protected def authConnector: AuthConnector = self.authConnector
    }
  }

  val response = SubscriptionResponse(
    etmpFormBundleNumber = "",
    amlsRefNo = "",
    registrationFee = 0,
    fpFee = None,
    premiseFee = 0,
    totalFees = 0,
    paymentReference = ""
  )

  "SubscriptionController" must {

    "post must return the response from the service correctly" in new Fixture {

      when {
        controller.subscriptionService.subscribe(any(), any(), any())
      } thenReturn Future.successful(response)

      val result = controller.post()(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe  Some(controllers.routes.ConfirmationController.get.url)
    }
  }
}
