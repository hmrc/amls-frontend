package controllers

import config.ApplicationConfig
import models.{Country, SubscriptionResponse}
import models.businesscustomer.{Address, ReviewDetails}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.Helpers._
import services.{AuthEnrolmentsService, LandingService}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class LandingControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new LandingController {
      override val enrolmentsService = mock[AuthEnrolmentsService]
      override val landingService = mock[LandingService]
      override val authConnector = self.authConnector
    }
  }

  "LandingController" must {

    "load the correct view after calling get" when {

      "the landing service has a saved form and " when {
        "the form has not been submitted" in new Fixture {
          when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(Some(CacheMap("", Map.empty)))
          when(controller.enrolmentsService.amlsRegistrationNumber(any(),any(),any())).thenReturn(Future.successful(None))
          val result = controller.get()(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.routes.StatusController.get().url)
        }

        "the form has been submitted" in new Fixture {
          val cacheMap = mock[CacheMap]
          when(cacheMap.getEntry[SubscriptionResponse](SubscriptionResponse.key))
            .thenReturn(Some(SubscriptionResponse("","",1.00,None,1.00,1.00,"")))
          when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(Some(cacheMap))
          when(controller.enrolmentsService.amlsRegistrationNumber(any(),any(),any())).thenReturn(Future.successful(None))
          val result = controller.get()(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.routes.StatusController.get().url)
        }
      }



      "the landing service has no saved form and " when {

        "the landing service has valid review details" in new Fixture {

          val details = Some(ReviewDetails(businessName = "Test",
                                           businessType = None,
                                           businessAddress = Address("Line 1", "Line 2", None, None, None, Country("United Kingdom", "GB")),
                                           safeId = ""))

          when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(None)
          when(controller.landingService.reviewDetails(any(), any())).thenReturn(Future.successful(details))
          when(controller.landingService.updateReviewDetails(any())(any(), any(), any())).thenReturn(Future.successful(mock[CacheMap]))
          when(controller.enrolmentsService.amlsRegistrationNumber(any(),any(),any())).thenReturn(Future.successful(None))
          val result = controller.get()(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.businessmatching.routes.BusinessTypeController.get().url)
        }

        "the landing service has no valid review details" in new Fixture {
          when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(None)
          when(controller.landingService.reviewDetails(any(), any())).thenReturn(Future.successful(None))
          when(controller.enrolmentsService.amlsRegistrationNumber(any(),any(),any())).thenReturn(Future.successful(None))
          val result = controller.get()(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(ApplicationConfig.businessCustomerUrl)
        }

        "the user has an AMLS enrolment" in new Fixture {
          when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(None)
          when(controller.landingService.reviewDetails(any(), any())).thenReturn(Future.successful(None))
          when(controller.enrolmentsService.amlsRegistrationNumber(any(),any(),any())).thenReturn(Future.successful(Some("amlsRegNo")))
          val result = controller.get()(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.routes.StatusController.get().url)
        }

      }


    }
  }
}