package controllers

import config.AMLSAuthConnector
import models.confirmation.Currency
import models.status.{SubmissionDecisionApproved, SubmissionReady, SubmissionReadyForReview}
import models.{ReadStatusResponse, SubscriptionResponse}
import org.joda.time.LocalDateTime
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.{StatusService, SubmissionService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture

import scala.concurrent.Future

class ConfirmationControllerSpec extends PlaySpec with OneAppPerSuite {

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new ConfirmationController {
      override protected val authConnector = self.authConnector
      override private[controllers] val submissionService = mock[SubmissionService]
      override val statusService: StatusService = mock[StatusService]
    }

    val paymentRefNo = "XA111123451111"

    val response = SubscriptionResponse(
      etmpFormBundleNumber = "",
      amlsRefNo = "",
      registrationFee = 0,
      fPFee = None,
      premiseFee = 0,
      totalFees = 0,
      paymentReference = paymentRefNo
    )

    protected val mockCacheMap = mock[CacheMap]

    when(controller.submissionService.getSubscription(any(), any(), any()))
      .thenReturn(Future.successful((paymentRefNo, Currency.fromInt(0), Seq())))

  }

  "ConfirmationController" must {

    "notify user of progress if application has not already been submitted" in new Fixture {

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionReady))

      val result = controller.get()(request)
      status(result) mustBe OK
      Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your application")
      contentAsString(result) must include(paymentRefNo)
    }

    "notify user of amendment if application has already been submitted but not approved with difference" in new Fixture {

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionReadyForReview))

      when(controller.submissionService.getAmendment(any(), any(), any()))
        .thenReturn(Future.successful(Some((Some(paymentRefNo), Currency.fromInt(0), Seq(), Some(Currency.fromInt(100))))))

      val result = controller.get()(request)
      status(result) mustBe OK
      Jsoup.parse(contentAsString(result)).title must include("You’ve submitted an updated application")
      contentAsString(result) must include(Messages("confirmation.amendment.fee"))
      contentAsString(result) must include(Messages("confirmation.amendment.thankyou.p"))
      contentAsString(result) must include(Messages("confirmation.amendment.previousfees.p"))
      contentAsString(result) must include(paymentRefNo)
    }

    "notify user of variation if application has been submitted and approved and fees have been accrued" in new Fixture {

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(SubmissionDecisionApproved))

      when(controller.submissionService.getVariation(any(), any(), any()))
        .thenReturn(Future.successful(Some((Some(paymentRefNo), Currency.fromInt(100), Seq()))))

      val result = controller.get()(request)
      status(result) mustBe OK
      Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your updated information")
      contentAsString(result) must include(Messages("confirmation.amendment.fee"))
      contentAsString(result) must include(Messages("confirmation.amendment.thankyou.p"))
      contentAsString(result) must include(Messages("confirmation.amendment.previousfees.p"))
      contentAsString(result) must include(paymentRefNo)
    }

    "notify user there is no fee" when {

      "an amendment has difference(/Some(0))" in new Fixture {

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReadyForReview))

        when(controller.submissionService.getAmendment(any(), any(), any()))
          .thenReturn(Future.successful(Some((Some(paymentRefNo), Currency.fromInt(0), Seq(), Some(Currency.fromInt(0))))))

        val result = controller.get()(request)
        status(result) mustBe OK
        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted an updated application")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(Messages("confirmation.amendment.previousfees.p"))
      }
      "an amendment has no difference(/None)" in new Fixture {

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReadyForReview))

        when(controller.submissionService.getAmendment(any(), any(), any()))
          .thenReturn(Future.successful(Some((Some(paymentRefNo), Currency.fromInt(0), Seq(), None))))

        val result = controller.get()(request)
        status(result) mustBe OK
        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted an updated application")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(Messages("confirmation.amendment.previousfees.p"))
      }

      "an amendment has no payment reference" in new Fixture {

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReadyForReview))

        when(controller.submissionService.getAmendment(any(), any(), any()))
          .thenReturn(Future.successful(Some((None, Currency.fromInt(0), Seq(), None))))

        val result = controller.get()(request)
        status(result) mustBe OK
        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted an updated application")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(Messages("confirmation.amendment.previousfees.p"))
      }

      "a variation has no payment reference" in new Fixture {

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        when(controller.submissionService.getVariation(any(), any(), any()))
          .thenReturn(Future.successful(Some((None, Currency.fromInt(0), Seq()))))

        val result = controller.get()(request)
        status(result) mustBe OK
        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your updated information")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(Messages("confirmation.amendment.previousfees.p"))
      }

      "a variation without the addition of tp or rp" in new Fixture {

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        when(controller.submissionService.getVariation(any(), any(), any()))
          .thenReturn(Future.successful(Some((Some(""), Currency.fromInt(0), Seq()))))

        val result = controller.get()(request)
        status(result) mustBe OK
        Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your updated information")
        contentAsString(result) must include(Messages("confirmation.no.fee"))
        contentAsString(result) must include(Messages("confirmation.amendment.previousfees.p"))
      }

    }
  }
}