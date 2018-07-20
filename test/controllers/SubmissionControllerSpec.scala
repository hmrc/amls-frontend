/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers

import exceptions._
import generators.AmlsReferenceNumberGenerator
import models.renewal.Renewal
import models.status._
import models.{AmendVariationRenewalResponse, SubmissionResponse, SubscriptionFees, SubscriptionResponse}
import org.joda.time.LocalDate
import org.jsoup._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import play.api.test.Helpers._
import services.{RenewalService, StatusService, SubmissionService}
import uk.gov.hmrc.http.{HttpResponse, Upstream5xxResponse}
import utils.{AuthorisedFixture, AmlsSpec}
import views.ParagraphHelpers

import scala.concurrent.Future

class SubmissionControllerSpec extends AmlsSpec with ScalaFutures with AmlsReferenceNumberGenerator {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new SubmissionController (
      mock[SubmissionService],
      mock[StatusService],
      mock[RenewalService],
      self.authConnector
    )
  }

  val response = SubscriptionResponse(
    etmpFormBundleNumber = "",
    amlsRefNo = "", Some(SubscriptionFees(
    registrationFee = 0,
    fpFee = None,
    fpFeeRate = None,
    premiseFee = 0,
    premiseFeeRate = None,
    totalFees = 0,
    paymentReference = ""))
  )

  val amendmentResponse = AmendVariationRenewalResponse(
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

  "SubmissionController" when {

    "subscribing" must {

      "return to the confirmation page on first submission" in new Fixture {

        when {
          controller.subscriptionService.subscribe(any(), any(), any())
        } thenReturn Future.successful(response)

        when(controller.statusService.getStatus(any(), any(), any())).thenReturn(Future.successful(SubmissionReady))

        val result = controller.post()(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.ConfirmationController.get.url)
      }

      "return to the landing controller when recovers from duplicate response" in new Fixture {
        when {
          controller.subscriptionService.subscribe(any(), any(), any())
        } thenReturn Future.successful(response.copy(previouslySubmitted = Some(true)))

        when(controller.statusService.getStatus(any(), any(), any())).thenReturn(Future.successful(SubmissionReady))

        val result = controller.post()(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.LandingController.get().url)
      }
    }

    "post must return the response from the service correctly when Submission Ready for review" in new Fixture {

      when {
        controller.subscriptionService.update(any(), any(), any())
      } thenReturn Future.successful(amendmentResponse)

      when(controller.statusService.getStatus(any(), any(), any())).thenReturn(Future.successful(SubmissionReadyForReview))

      val result = controller.post()(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.ConfirmationController.get.url)
    }

    "show the correct help page when a duplicate enrolment error is encountered while trying to enrol the user" in new Fixture with ParagraphHelpers {
      val msg = "HMRC-MLR-ORG duplicate enrolment"

      when {
        controller.subscriptionService.subscribe(any(), any(), any())
      } thenReturn Future.failed(DuplicateEnrolmentException(msg, Upstream5xxResponse(msg, BAD_GATEWAY, BAD_GATEWAY)))

      when {
        controller.statusService.getStatus(any(), any(), any())
      } thenReturn Future.successful(SubmissionReady)

      val result = controller.post()(request)

      status(result) mustBe OK

      implicit val doc = Jsoup.parse(contentAsString(result))
      validateParagraphizedContent("error.submission.duplicate_enrolment.content")
    }

    "show the correct help page when a duplicate subscription error is encountered" in new Fixture with ParagraphHelpers {
      val msg = "HMRC-MLR-ORG duplicate subscription"

      when {
        controller.subscriptionService.subscribe(any(), any(), any())
      } thenReturn Future.failed(DuplicateSubscriptionException(msg))

      when {
        controller.statusService.getStatus(any(), any(), any())
      } thenReturn Future.successful(SubmissionReady)

      val result = controller.post()(request)

      status(result) mustBe OK

      implicit val doc = Jsoup.parse(contentAsString(result))
      validateParagraphizedContent("error.submission.duplicate_submission.content")
    }

    "show the correct help page when an error is encountered while trying to enrol the user" in new Fixture with ParagraphHelpers {
      val msg = "invalid credentials"

      when {
        controller.statusService.getStatus(any(), any(), any())
      } thenReturn Future.successful(SubmissionReady)

      when {
        controller.subscriptionService.subscribe(any(), any(), any())
      } thenReturn Future.failed(InvalidEnrolmentCredentialsException(msg, Upstream5xxResponse(msg, BAD_GATEWAY, BAD_GATEWAY)))

      val result = controller.post()(request)

      status(result) mustBe OK

      implicit val doc = Jsoup.parse(contentAsString(result))
      validateParagraphizedContent("error.submission.wrong_credentials.content")
    }
  }

  it when {
    "Submission is approved" must {
      "call the variation method on the service" in new Fixture {
        when {
          controller.subscriptionService.variation(any(), any(), any())
        } thenReturn Future.successful(mock[AmendVariationRenewalResponse])

        when(controller.statusService.getStatus(any(), any(), any())).thenReturn(Future.successful(SubmissionDecisionApproved))

        val result = controller.post()(request)

        whenReady(result) { _ =>
          verify(controller.subscriptionService).variation(any(), any(), any())
        }
      }


      "Redirect to the correct confirmation page" in new Fixture {
        when {
          controller.subscriptionService.variation(any(), any(), any())
        } thenReturn Future.successful(amendmentResponse)

        when(controller.statusService.getStatus(any(), any(), any())).thenReturn(Future.successful(SubmissionDecisionApproved))

        val result = controller.post()(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.ConfirmationController.get.url)
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
        } thenReturn Future.successful(mock[AmendVariationRenewalResponse])

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

    "Submission is in renewal amendment status" must {
      "call the renewal amendment method on the service" in new Fixture {

        when {
          controller.subscriptionService.renewalAmendment(any())(any(), any(), any())
        } thenReturn Future.successful(mock[SubmissionResponse])

        when {
          controller.statusService.getStatus(any(), any(), any())
        } thenReturn Future.successful(RenewalSubmitted(Some(LocalDate.now.plusDays(15))))

        when {
          controller.renewalService.getRenewal(any(), any(), any())
        } thenReturn Future.successful(Some(mock[Renewal]))

        val result = controller.post()(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.ConfirmationController.get().url)
      }
    }
  }
}
