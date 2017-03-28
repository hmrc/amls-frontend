package controllers

import models.renewal.Renewal
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionReady, SubmissionReadyForReview}
import models.{AmendVariationResponse, SubmissionResponse, SubscriptionResponse}
import org.joda.time.LocalDate
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import play.api.test.Helpers._
import services.{RenewalService, StatusService, SubmissionService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class SubmissionControllerSpec extends GenericTestHelper with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new SubmissionController {
      override private[controllers] val subscriptionService: SubmissionService = mock[SubmissionService]
      override protected def authConnector: AuthConnector = self.authConnector
      override private[controllers] val statusService: StatusService = mock[StatusService]
      override private[controllers] val renewalService = mock[RenewalService]
    }

  }

  val response = SubscriptionResponse(
    etmpFormBundleNumber = "",
    amlsRefNo = "",
    registrationFee = 0,
    fpFee = None,
    fpFeeRate = None,
    premiseFee = 0,
    premiseFeeRate = None,
    totalFees = 0,
    paymentReference = ""
  )

  val amendmentResponse = AmendVariationResponse(
    processingDate = "",
    etmpFormBundleNumber = "",
    registrationFee = 0,
    fpFee = Some(0),
    fpFeeRate = Some(0),
    premiseFee = 0,
    premiseFeeRate = None,
    totalFees = 0,
    paymentReference = Some(""),
    difference = Some(0)
  )

  "SubmissionController" must {

    "post must return the response from the service correctly when Submission Ready" in new Fixture {

      when {
        controller.subscriptionService.subscribe(any(), any(), any())
      } thenReturn Future.successful(response)

      when(controller.statusService.getStatus(any(),any(),any())).thenReturn(Future.successful(SubmissionReady))

      val result = controller.post()(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe  Some(controllers.routes.ConfirmationController.get.url)
    }

    "post must return the response from the service correctly when Submission Ready for review" in new Fixture {

      when {
        controller.subscriptionService.update(any(), any(), any())
      } thenReturn Future.successful(amendmentResponse)

      when(controller.statusService.getStatus(any(),any(),any())).thenReturn(Future.successful(SubmissionReadyForReview))

      val result = controller.post()(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe  Some(controllers.routes.ConfirmationController.get.url)
    }
  }

  it when {
    "Submission is approved" must {
      "call the variation method on the service" in new Fixture {
        when {
          controller.subscriptionService.variation(any(), any(), any())
        } thenReturn Future.successful(mock[AmendVariationResponse])

        when(controller.statusService.getStatus(any(), any(), any())).thenReturn(Future.successful(SubmissionDecisionApproved))

        val result = controller.post()(request)

        whenReady(result) { _ =>
          verify(controller.subscriptionService).variation(any(), any(), any())
        }
      }


      "Redirect to the correct confirmation page" in new Fixture{
        when {
          controller.subscriptionService.variation(any(), any(), any())
        } thenReturn Future.successful(amendmentResponse)

        when(controller.statusService.getStatus(any(),any(),any())).thenReturn(Future.successful(SubmissionDecisionApproved))

        val result = controller.post()(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe  Some(controllers.routes.ConfirmationController.get.url)
      }
    }

    "Submission is in renewal status" must {
      "call the renewal method on the service" in new Fixture {

        when {
          controller.subscriptionService.renewal(any())(any(), any(), any())
        } thenReturn Future.successful(mock[SubmissionResponse])

        when {
          controller.statusService.getStatus(any(), any(), any())
        } thenReturn Future.successful(ReadyForRenewal(Some(LocalDate.now.plusDays(15))))

        when {
          controller.renewalService.getRenewal(any(), any(), any())
        } thenReturn Future.successful(Some(mock[Renewal]))

        val result = controller.post()(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.ConfirmationController.get().url)

        verify(controller.renewalService).getRenewal(any(), any(), any())

      }

      "do a variation if user is in renewal period but has no renewal object" in new Fixture {

        when {
          controller.subscriptionService.variation(any(), any(), any())
        } thenReturn Future.successful(mock[AmendVariationResponse])

        when {
          controller.statusService.getStatus(any(), any(), any())
        } thenReturn Future.successful(ReadyForRenewal(Some(LocalDate.now.plusDays(15))))

        when {
          controller.renewalService.getRenewal(any(), any(), any())
        } thenReturn Future.successful(None)

        val result = await(controller.post()(request))

        verify(controller.subscriptionService).variation(any(), any(), any())
        verify(controller.subscriptionService, never()).renewal(any())(any(), any(), any())
      }
    }
  }
}
