package controllers

import config.ApplicationConfig
import models.businesscustomer.{Address, ReviewDetails}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.Helpers._
import services.LandingService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class LandingControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new LandingController {
      override val landingService = mock[LandingService]
      override val authConnector = self.authConnector
    }
  }

  "LandingController" must {

    "load the correct view after calling get" when {

      "the landing service has a saved form" in new Fixture {
        when(controller.landingService.hasSavedForm(any(), any(), any())) thenReturn Future.successful(true)
        val result = controller.get()(request)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get().url)
      }

      "the landing service has no saved form and " when {

        "the landing service has valid review details" in new Fixture {

          val details = Some(ReviewDetails(businessName = "Test",
                                           businessType = None,
                                           businessAddress = Address("Line 1", "Line 2", None, None, None, "Country"),
                                           safeId = ""))

          when(controller.landingService.hasSavedForm(any(), any(), any())) thenReturn Future.successful(false)
          when(controller.landingService.reviewDetails(any(), any())).thenReturn(Future.successful(details))
          when(controller.landingService.updateReviewDetails(any())(any(), any(), any())).thenReturn(Future.successful(mock[CacheMap]))
          val result = controller.get()(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.businessmatching.routes.BusinessTypeController.get().url)
        }

        "the landing service has no valid review details" in new Fixture {
          when(controller.landingService.hasSavedForm(any(), any(), any())) thenReturn Future.successful(false)
          when(controller.landingService.reviewDetails(any(), any())).thenReturn(Future.successful(None))
          val result = controller.get()(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(ApplicationConfig.businessCustomerUrl)
        }

      }
    }
  }
}