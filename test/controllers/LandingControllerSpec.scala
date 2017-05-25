/*
 * Copyright 2017 HM Revenue & Customs
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

import config.ApplicationConfig
import models.aboutthebusiness.AboutTheBusiness
import models.asp.Asp
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching._
import models.estateagentbusiness.EstateAgentBusiness
import models.hvd.Hvd
import models.moneyservicebusiness.MoneyServiceBusiness
import models.renewal.Renewal
import models.responsiblepeople.ResponsiblePeople
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import models.{Country, SubscriptionFees, SubscriptionResponse}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.MustMatchers
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.Helpers._
import play.api.test.{FakeApplication, FakeRequest}
import services.{AuthEnrolmentsService, LandingService}
import uk.gov.hmrc.http.cache.client.{CacheMap, ShortLivedCache}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.logging.Authorization
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import utils.AuthorisedFixture

import scala.concurrent.{ExecutionContext, Future}

class LandingControllerWithoutAmendmentsSpec extends GenericTestHelper with MockitoSugar {

  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.amendments" -> false))

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)
    val controller = new LandingController {
      override val enrolmentsService = mock[AuthEnrolmentsService]
      override val landingService = mock[LandingService]
      override val authConnector = self.authConnector
      override val shortLivedCache = mock[ShortLivedCache]
    }
  }

  "LandingController" must {

    "load the correct view after calling get" when {

      "the landing service has a saved form and " when {
        "the form has not been submitted" in new Fixture {
          when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(Some(CacheMap("", Map.empty)))
          when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(None))

          val complete = mock[BusinessMatching]
          val emptyCacheMap = mock[CacheMap]

          when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(Some(emptyCacheMap))
          when(complete.isComplete) thenReturn true
          when(emptyCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))

          val result = controller.get()(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.routes.StatusController.get().url)
        }

        "the form has been submitted" in new Fixture {
          val cacheMap = mock[CacheMap]

          val complete = mock[BusinessMatching]

          when(complete.isComplete) thenReturn true
          when(cacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))

          when(cacheMap.getEntry[SubscriptionResponse](SubscriptionResponse.key))
            .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, 1.0, None, 1.0)))))
          when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(Some(cacheMap))
          when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(None))

          val result = controller.get()(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.routes.StatusController.get().url)
        }
      }


      "the landing service has no saved form and " when {

        "the landing service has valid review details" in new Fixture {

          val details = Some(ReviewDetails(businessName = "Test",
            businessType = None,
            businessAddress = Address("Line 1", "Line 2", None, None, Some("AA11AA"), Country("United Kingdom", "GB")),
            safeId = ""))

          when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(None)
          when(controller.landingService.reviewDetails(any(), any(), any())).thenReturn(Future.successful(details))
          when(controller.landingService.updateReviewDetails(any())(any(), any(), any())).thenReturn(Future.successful(mock[CacheMap]))
          when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(None))
          val result = controller.get()(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.businessmatching.routes.BusinessTypeController.get().url)
        }

        "the landing service has review details with invalid postcode" in new Fixture {

          val details = Some(ReviewDetails(businessName = "Test",
            businessType = None,
            businessAddress = Address("Line 1", "Line 2", None, None, Some("aa1 $ aa156"), Country("United Kingdom", "GB")),
            safeId = ""))

          when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(None)
          when(controller.landingService.reviewDetails(any(), any(), any())).thenReturn(Future.successful(details))
          when(controller.landingService.updateReviewDetails(any())(any(), any(), any())).thenReturn(Future.successful(mock[CacheMap]))
          when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(None))
          val result = controller.get()(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.businessmatching.routes.ConfirmPostCodeController.get().url)
        }

        "the landing service has no valid review details" in new Fixture {
          when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(None)
          when(controller.landingService.reviewDetails(any(), any(), any())).thenReturn(Future.successful(None))
          when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(None))
          val result = controller.get()(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(ApplicationConfig.businessCustomerUrl)
        }

        "the user has an AMLS enrolment" in new Fixture {
          when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(None)
          when(controller.landingService.reviewDetails(any(), any(), any())).thenReturn(Future.successful(None))
          when(controller.enrolmentsService.amlsRegistrationNumber(any(), any(), any())).thenReturn(Future.successful(Some("amlsRegNo")))
          val result = controller.get()(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.routes.StatusController.get().url)
        }

      }

      "go to the beginning of pre-application" when {
        "there is no data in BusinessMatching" in new Fixture {

          val emptyCacheMap = mock[CacheMap]

          when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(Some(CacheMap("", Map.empty)))
          when(emptyCacheMap.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(None)
          //when(emptyCacheMap.getEntry[AboutTheBusiness](AboutTheBusiness.key)).thenReturn(None)

          val result = controller.get()(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.routes.LandingController.get().url)

        }
      }

      "go to the beginning of pre-application" when {
        "there is data in BusinessMatching but the pre-application is incomplete" in new Fixture {

          val testBusinessMatching = BusinessMatching()

          val emptyCacheMap = mock[CacheMap]

          when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(Some(CacheMap("", Map.empty)))
          when(emptyCacheMap.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(Some(testBusinessMatching))

          val result = controller.get()(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.routes.LandingController.get().url)

        }
      }

      "pre application must remove save4later" when {
        "the business matching is incomplete" in new Fixture {
          val cachmap = mock[CacheMap]
          val httpResponse = mock[HttpResponse]

          val complete = mock[BusinessMatching]

          when(httpResponse.status) thenReturn (NO_CONTENT)

          when(controller.shortLivedCache.remove(any())(any())) thenReturn Future.successful(httpResponse)

          when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(Some(cachmap))
          when(complete.isComplete) thenReturn false
          when(cachmap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))

          val result = controller.get()(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.routes.LandingController.get().url)

        }
      }

      "pre application must throw an exception" when {
        "the business matching is incomplete" in new Fixture {
          val cachmap = mock[CacheMap]
          val httpResponse = mock[HttpResponse]

          val complete = mock[BusinessMatching]

          when(httpResponse.status) thenReturn (BAD_REQUEST)

          when(controller.shortLivedCache.remove(any())(any())) thenReturn Future.successful(httpResponse)

          when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(Some(cachmap))
          when(complete.isComplete) thenReturn false
          when(cachmap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))

          a[Exception] must be thrownBy {
            await(controller.get()(request))
          }
        }
      }
    }
  }
}

class LandingControllerWithAmendmentsSpec extends GenericTestHelper with MockitoSugar with MustMatchers {

  val businessCustomerUrl = "TestUrl"

  override lazy val app = FakeApplication(additionalConfiguration = Map(
    "microservice.services.feature-toggle.amendments" -> true
  ))


  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)
    val controller = new LandingController {
      override val landingService = mock[LandingService]
      override val authConnector = self.authConnector
      override val enrolmentsService = mock[AuthEnrolmentsService]
    }

    when(controller.landingService.refreshCache(any())(any(), any(), any())).thenReturn(Future.successful(mock[CacheMap]))
  }


  def setUpMocksForNoEnrolment(controller: LandingController) = {
    when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(None))
  }

  def setUpMocksForAnEnrolmentExists(controller: LandingController) = {
    when(controller.enrolmentsService.amlsRegistrationNumber(any[AuthContext], any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(Some("TESTREGNO")))
  }

  def setUpMocksForNoDataInSaveForLater(controller: LandingController) = {
    when(controller.landingService.cacheMap(any[HeaderCarrier], any[ExecutionContext], any[AuthContext]))
      .thenReturn(Future.successful(None))
  }

  def setUpMocksForDataExistsInKeystore(controller: LandingController) = {
    val reviewDetails = ReviewDetails(
      "Business Name",
      None,
      Address("Line1", "Line2", None, None, Some("AA11AA"), Country("United Kingdom", "UK")),
      "testSafeId")

    when(controller.landingService.reviewDetails(any[HeaderCarrier], any[ExecutionContext], any[Request[_]]))
      .thenReturn(Future.successful(Some(reviewDetails)))

    reviewDetails
  }

  def setUpMocksForNoDataInKeyStore(controller: LandingController) = {
    when(controller.landingService.reviewDetails(any[HeaderCarrier], any[ExecutionContext], any[Request[_]]))
      .thenReturn(Future.successful(None))
  }

  def setUpMocksForDataExistsInSaveForLater(controller: LandingController, testData: CacheMap = mock[CacheMap]) = {
    when(controller.landingService.cacheMap(any[HeaderCarrier], any[ExecutionContext], any[AuthContext]))
      .thenReturn(Future.successful(Some(testData)))
  }

  def buildTestCacheMap(hasChanged: Boolean, includesResponse: Boolean): CacheMap = {
    val result = mock[CacheMap]
    val testASP = Asp(hasChanged = hasChanged)
    val testAboutTheBusiness = AboutTheBusiness(hasChanged = hasChanged)
    val testBankDetails = Seq(BankDetails(hasChanged = hasChanged))
    val testBusinessActivities = BusinessActivities(hasChanged = hasChanged)
    val testBusinessMatching = BusinessMatching(hasChanged = hasChanged)
    val testEstateAgentBusiness = EstateAgentBusiness(hasChanged = hasChanged)
    val testMoneyServiceBusiness = MoneyServiceBusiness(hasChanged = hasChanged)
    val testResponsiblePeople = Seq(ResponsiblePeople(hasChanged = hasChanged))
    val testSupervision = Supervision(hasChanged = hasChanged)
    val testTcsp = Tcsp(hasChanged = hasChanged)
    val testTradingPremises = Seq(TradingPremises(hasChanged = hasChanged))
    val testHvd = Hvd(hasChanged = hasChanged)
    val testRenewal = Renewal(hasChanged = hasChanged)

    when(result.getEntry[Asp](Asp.key)).thenReturn(Some(testASP))
    when(result.getEntry[AboutTheBusiness](AboutTheBusiness.key)).thenReturn(Some(testAboutTheBusiness))
    when(result.getEntry[Seq[BankDetails]](meq(BankDetails.key))(any())).thenReturn(Some(testBankDetails))
    when(result.getEntry[BusinessActivities](BusinessActivities.key)).thenReturn(Some(testBusinessActivities))
    when(result.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(Some(testBusinessMatching))
    when(result.getEntry[EstateAgentBusiness](EstateAgentBusiness.key)).thenReturn(Some(testEstateAgentBusiness))
    when(result.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)).thenReturn(Some(testMoneyServiceBusiness))
    when(result.getEntry[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key))(any())).thenReturn(Some(testResponsiblePeople))
    when(result.getEntry[Supervision](Supervision.key)).thenReturn(Some(testSupervision))
    when(result.getEntry[Tcsp](Tcsp.key)).thenReturn(Some(testTcsp))
    when(result.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))(any())).thenReturn(Some(testTradingPremises))
    when(result.getEntry[Hvd](Hvd.key)).thenReturn(Some(testHvd))
    when(result.getEntry[Renewal](Renewal.key)).thenReturn(Some(testRenewal))

    if (includesResponse) {
      val testResponse = SubscriptionResponse(
        "TESTFORMBUNDLENUMBER",
        "TESTAMLSREFNNO", Some(SubscriptionFees(
          "TESTPAYMENTREF",
          100.45,
          None,
          None,
          123.78,
          None,
          17623.76
        ))
      )

      when(result.getEntry[SubscriptionResponse](SubscriptionResponse.key))
        .thenReturn(Some(testResponse))

    }

    result
  }

  "show landing page without authorisation" in new Fixture {

    val result = controller.start()(FakeRequest().withSession())
    status(result) must be(OK)

  }

  "direct to the service when authorised" in new Fixture {
    val result = controller.start()(request)
    status(result) must be(SEE_OTHER)
  }

  "Landing Controller" when {
    "an enrolment exists and" when {
      "there is data in S4L and" when {
        "The Save 4 Later data does not contain any sections" should {
          "data has not changed" should {
            "refresh from API5 and redirect to status controller" in new Fixture {
              setUpMocksForAnEnrolmentExists(controller)
              val emptyCacheMap = mock[CacheMap]
              when(emptyCacheMap.getEntry[Asp](Asp.key)).thenReturn(None)
              when(emptyCacheMap.getEntry[AboutTheBusiness](AboutTheBusiness.key)).thenReturn(None)
              when(emptyCacheMap.getEntry[Seq[BankDetails]](meq(BankDetails.key))(any())).thenReturn(None)
              when(emptyCacheMap.getEntry[BusinessActivities](BusinessActivities.key)).thenReturn(None)
              when(emptyCacheMap.getEntry[BusinessMatching](BusinessMatching.key)).thenReturn(None)
              when(emptyCacheMap.getEntry[EstateAgentBusiness](EstateAgentBusiness.key)).thenReturn(None)
              when(emptyCacheMap.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)).thenReturn(None)
              when(emptyCacheMap.getEntry[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key))(any())).thenReturn(None)
              when(emptyCacheMap.getEntry[Supervision](Supervision.key)).thenReturn(None)
              when(emptyCacheMap.getEntry[Tcsp](Tcsp.key)).thenReturn(None)
              when(emptyCacheMap.getEntry[Seq[TradingPremises]](meq(TradingPremises.key))(any())).thenReturn(None)
              when(emptyCacheMap.getEntry[Hvd](Hvd.key)).thenReturn(None)
              when(emptyCacheMap.getEntry[Renewal](Renewal.key)).thenReturn(None)
              setUpMocksForDataExistsInSaveForLater(controller, emptyCacheMap)

              val result = controller.get()(request)

              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(controllers.routes.StatusController.get().url))
              verify(controller.landingService, atLeastOnce()).refreshCache(any())(any[AuthContext], any[HeaderCarrier], any[ExecutionContext])
            }
          }
        }

        "data has changed and" when {
          "there is a subscription response" should {
            "refresh from API5 and redirect to status controller" in new Fixture {
              setUpMocksForAnEnrolmentExists(controller)
              setUpMocksForDataExistsInSaveForLater(controller, buildTestCacheMap(true, true))

              val result = controller.get()(request.withHeaders("test-context" -> "ESCS"))

              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(controllers.routes.StatusController.get().url))

              verify(controller.landingService).refreshCache(any())(any[AuthContext], any[HeaderCarrier], any[ExecutionContext])
            }


          }

          "there is no subscription response" should {
            "redirect to status controller without refreshing API5" in new Fixture {
              setUpMocksForAnEnrolmentExists(controller)
              setUpMocksForDataExistsInSaveForLater(controller, buildTestCacheMap(true, false))

              val result = controller.get()(request)

              verify(controller.landingService, never()).refreshCache(any())(any[AuthContext], any[HeaderCarrier], any[ExecutionContext])
              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(controllers.routes.StatusController.get().url))
            }
          }
        }

        "data has not changed" should {
          "refresh from API5 and redirect to status controller" in new Fixture {
            setUpMocksForAnEnrolmentExists(controller)
            setUpMocksForDataExistsInSaveForLater(controller, buildTestCacheMap(false, false))

            val result = controller.get()(request)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.routes.StatusController.get().url))
            verify(controller.landingService, atLeastOnce()).refreshCache(any())(any[AuthContext], any[HeaderCarrier], any[ExecutionContext])
          }
        }
      }

      "there is no data in S4L" should {
        "refresh from API5 and redirect to status controller" in new Fixture {
          setUpMocksForAnEnrolmentExists(controller)
          setUpMocksForNoDataInSaveForLater(controller)

          val result = controller.get()(request)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.routes.StatusController.get().url))
          verify(controller.landingService, atLeastOnce()).refreshCache(any())(any[AuthContext], any[HeaderCarrier], any[ExecutionContext])
        }
      }
    }

    "an enrolment does not exist" when {
      "there is data in S4L " should {
        "do not refresh API5 and redirect to status controller" in new Fixture {

          val complete = mock[BusinessMatching]
          val emptyCacheMap = mock[CacheMap]

          setUpMocksForNoEnrolment(controller)


          when(controller.landingService.cacheMap(any(), any(), any())) thenReturn Future.successful(Some(emptyCacheMap))
          when(complete.isComplete) thenReturn true
          when(emptyCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))

          val result = controller.get()(request)

          verify(controller.landingService, never()).refreshCache(any())(any[AuthContext], any[HeaderCarrier], any[ExecutionContext])
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.routes.StatusController.get().url))
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

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.businessmatching.routes.BusinessTypeController.get().url))

            Mockito.verify(controller.landingService, times(1))
              .updateReviewDetails(any[ReviewDetails])(any[HeaderCarrier], any[ExecutionContext], any[AuthContext])
          }
        }

        "there is no data in keystore" should {
          "redirect to business customer" in new Fixture {
            setUpMocksForNoEnrolment(controller)
            setUpMocksForNoDataInSaveForLater(controller)
            setUpMocksForNoDataInKeyStore(controller)

            val result = controller.get()(request)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("http://localhost:9923/business-customer/amls"))
          }
        }
      }
    }
  }
}
