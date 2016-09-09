package controllers

import config.ApplicationConfig
import models.asp.Asp
import models.moneyservicebusiness.MoneyServiceBusiness
import models.{Country, SubscriptionResponse}
import models.businesscustomer.{Address, ReviewDetails}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => meq}
import org.mockito.Mockito
import org.scalatest.mock.MockitoSugar
import org.scalatest.MustMatchers
import org.scalatest.fixture.WordSpec
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.FakeApplication
import play.api.test.Helpers._
import services.{AuthEnrolmentsService, LandingService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AuthorisedFixture

import scala.concurrent.{ExecutionContext, Future}

class LandingControllerWithoutAmendmentsSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  implicit override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.services.feature-toggle.amendments" -> false) )

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
            .thenReturn(Some(SubscriptionResponse("", "", 1.00, None, 1.00, 1.00, "")))
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

class LandingControllerWithAmendmentsSpec extends PlaySpec with OneAppPerSuite with MockitoSugar with MustMatchers {

  val businessCustomerUrl = "TestUrl"

  implicit override lazy val app = FakeApplication(additionalConfiguration = Map(
    "Test.microservice.services.feature-toggle.amendments" -> true,
    "Test.microservice.services.business-customer.url" -> businessCustomerUrl
  ))


  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new LandingController {
      override val landingService = mock[LandingService]
      override val authConnector = self.authConnector
      override val enrolmentsService = mock[AuthEnrolmentsService]
    }
  }

  def setUpMocksForNoEnrolment(controller : LandingController) = {
    when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(None))
  }

  def setUpMocksForAnEnrolmentExists(controller : LandingController) = {
    when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(Some("TESTREGNO")))
  }

  def setUpMocksForNoDataInSaveForLater(controller : LandingController) = {
    when(controller.landingService.cacheMap(any[HeaderCarrier], any[ExecutionContext], any[AuthContext]))
      .thenReturn(Future.successful(None))
  }

  def setUpMocksForDataExistsInKeystore(controller : LandingController) = {
    val reviewDetails = ReviewDetails(
      "Business Name",
      None,
      Address("Line1", "Line2", None, None, None, Country("United Kingdom", "UK")),
      "testSafeId")

    when(controller.landingService.reviewDetails(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(Some(reviewDetails)))

    reviewDetails
  }

  def setUpMocksForNoDataInKeyStore(controller : LandingController) = {
    when(controller.landingService.reviewDetails(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(None))
  }

  def setUpMocksForDataExistsInSaveForLater(controller : LandingController, testData : CacheMap = mock[CacheMap]) = {
    when(controller.landingService.cacheMap(any[HeaderCarrier], any[ExecutionContext], any[AuthContext]))
      .thenReturn(Future.successful(Some(testData)))
  }

  def buildTestCacheMap(hasChanged : Boolean, includesResponse : Boolean) : CacheMap = {
    val result = mock[CacheMap]

    if (hasChanged) {
      val testASP = Asp(hasChanged = true)

      when(result.getEntry[Asp](Asp.key))
       .thenReturn(Some(testASP))
    }

    if (includesResponse) {
      val testResponse = SubscriptionResponse(
        "TESTFORMBUNDLENUMBER",
          "TESTAMLSREFNNO",
          100.45,
          None,
          123.78,
          17623.76,
          "TESTPAYMENTREF"
      )

      when(result.getEntry[SubscriptionResponse](SubscriptionResponse.key))
        .thenReturn(Some(testResponse))

    }

    result
  }

  "Landing Controller" when {
    "an enrolment exists and" when {
      "there is data in S4L and" when {
        "data has changed and" when {
          "there is a subscription response" should {
            "refresh from API5 and redirect to status controller" in new Fixture {
              setUpMocksForAnEnrolmentExists(controller)
              setUpMocksForDataExistsInSaveForLater(controller, buildTestCacheMap(true, true))

              val result = controller.get()(request.withHeaders("test-context" ->"ESCS"))

              verify(controller.landingService, atLeastOnce()).refreshCache
              status(result) must be (SEE_OTHER)
              redirectLocation(result) must be (Some(controllers.routes.StatusController.get().url))
            }



          }

          "there is no subscription response" should {
            "redirect to status controller without refreshing API5" in new Fixture {
              setUpMocksForAnEnrolmentExists(controller)
              setUpMocksForDataExistsInSaveForLater(controller, buildTestCacheMap(true, false))

              val result = controller.get()(request)

              verify(controller.landingService, never()).refreshCache
              status(result) must be (SEE_OTHER)
              redirectLocation(result) must be (Some(controllers.routes.StatusController.get().url))
            }
          }
        }

        "data has not changed" should {
          "refresh from API5 and redirect to status controller" in new Fixture {
            setUpMocksForAnEnrolmentExists(controller)
            setUpMocksForDataExistsInSaveForLater(controller, buildTestCacheMap(false, false))

            val result = controller.get()(request)

            verify(controller.landingService, atLeastOnce()).refreshCache
            status(result) must be (SEE_OTHER)
            redirectLocation(result) must be (Some(controllers.routes.StatusController.get().url))
          }
        }
      }

      "there is no data in S4L" should {
        "refresh from API5 and redirect to status controller" in new Fixture {
          setUpMocksForAnEnrolmentExists(controller)
          setUpMocksForNoDataInSaveForLater(controller)

          val result = controller.get()(request)

          verify(controller.landingService, atLeastOnce()).refreshCache
          status(result) must be (SEE_OTHER)
          redirectLocation(result) must be (Some(controllers.routes.StatusController.get().url))
        }
      }
    }

    "an enrolment does not exist" when {
      "there is data in S4L " should {
        "do not refresh API5 and redirect to status controller" in new Fixture{
          setUpMocksForNoEnrolment(controller)
          setUpMocksForDataExistsInSaveForLater(controller)

          val result = controller.get()(request)

          verify(controller.landingService, never()).refreshCache
          status(result) must be (SEE_OTHER)
          redirectLocation(result) must be (Some(controllers.routes.StatusController.get().url))
        }
      }

      "there is no data in S4L" when {
        "there is data in keystore " should {
          "copy keystore data to S4L and redirect to business type controler" in new Fixture {
            setUpMocksForNoEnrolment(controller)
            setUpMocksForNoDataInSaveForLater(controller)
            val reviewDetails = setUpMocksForDataExistsInKeystore(controller)

            when(controller.landingService.updateReviewDetails(any[ReviewDetails])(any[HeaderCarrier], any[ExecutionContext], any[AuthContext]))
              .thenReturn(Future.successful(mock[CacheMap]))

            val result = controller.get()(request)

            Mockito.verify(controller.landingService, only())
              .updateReviewDetails(any[ReviewDetails])(any[HeaderCarrier], any[ExecutionContext], any[AuthContext])

            status(result) must be (SEE_OTHER)
            redirectLocation(result) must be (Some(controllers.businessmatching.routes.BusinessTypeController.get().url))

          }
        }

        "there is no data in keystore" should {
          "redirect to business customer" in new Fixture {
            setUpMocksForNoEnrolment(controller)
            setUpMocksForNoDataInSaveForLater(controller)
            setUpMocksForNoDataInKeyStore(controller)

            val result = controller.get()(request)
            status(result) must be (SEE_OTHER)
            redirectLocation(result) must be (Some(businessCustomerUrl))
          }
        }
      }
    }
  }
}
