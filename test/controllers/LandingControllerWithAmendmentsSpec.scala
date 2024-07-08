/*
 * Copyright 2024 HM Revenue & Customs
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
import connectors.DataCacheConnector
import controllers.actions.{SuccessfulAuthAction, SuccessfulAuthActionNoAmlsRefNo}
import generators.StatusGenerator
import models.businesscustomer.{Address, ReviewDetails}
import models.businessdetails.BusinessDetails
import models.businessmatching._
import models.eab.Eab
import models.responsiblepeople._
import models.status._
import models.tradingpremises.TradingPremises
import models.{status => _, _}
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.BodyParsers
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.cache.Cache
import services.{AuthEnrolmentsService, LandingService, StatusService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter
import utils.{AmlsSpec, AuthorisedFixture}
import views.html.Start

import scala.concurrent.{ExecutionContext, Future}

class LandingControllerWithAmendmentsSpec extends AmlsSpec with MockitoSugar with Matchers with StatusGenerator
  with ResponsiblePeopleValues with CacheValues {

  val businessCustomerUrl = "TestUrl"

  lazy val headerCarrierForPartialsConverter = app.injector.instanceOf[HeaderCarrierForPartialsConverter]

  trait Fixture { self =>

    val request = addToken(authRequest)
    val config = mock[ApplicationConfig]
    lazy val view = app.injector.instanceOf[Start]

    val controller = new LandingController(
      enrolmentsService = mock[AuthEnrolmentsService],
      landingService = mock[LandingService],
      authAction = SuccessfulAuthAction,
      auditConnector = mock[AuditConnector],
      cacheConnector = mock[DataCacheConnector],
      statusService = mock[StatusService],
      ds = commonDependencies,
      mcc = mockMcc,
      messagesApi = messagesApi,
      config = config,
      parser = mock[BodyParsers.Default],
      start = view,
      headerCarrierForPartialsConverter = headerCarrierForPartialsConverter,
      applicationCrypto = applicationCrypto)

    when(controller.landingService.refreshCache(any(), any[String](), any())(any(), any(), any())).thenReturn(Future.successful(mock[Cache]))

    when(controller.landingService.setAltCorrespondenceAddress(any(), any[String])).thenReturn(Future.successful(mock[Cache]))

    val completeATB = mock[BusinessDetails]

    def setUpMocksForDataExistsInSaveForLater(controller: LandingController, testData: Cache = mock[Cache]) = {
      when(controller.landingService.cacheMap(any[String])).thenReturn(Future.successful(Some(testData)))
      when(controller.landingService.initialiseGetWithAmendments(any[String])(any())).thenReturn(Future.successful(Some(testData)))
    }

    //noinspection ScalaStyle
  }

  trait FixtureNoAmlsNumber extends AuthorisedFixture {
    self =>

    val request = addToken(authRequest)
    val config = mock[ApplicationConfig]
    lazy val view = app.injector.instanceOf[Start]

    val controller = new LandingController(
      enrolmentsService = mock[AuthEnrolmentsService],
      landingService = mock[LandingService],
      authAction = SuccessfulAuthActionNoAmlsRefNo,
      auditConnector = mock[AuditConnector],
      cacheConnector = mock[DataCacheConnector],
      statusService = mock[StatusService],
      ds = commonDependencies,
      mcc = mockMcc,
      messagesApi = messagesApi,
      config = config,
      parser = mock[BodyParsers.Default],
      start = view,
      headerCarrierForPartialsConverter = headerCarrierForPartialsConverter,
      applicationCrypto = applicationCrypto)

    when(controller.landingService.refreshCache(any(), any[String](), any())(any(), any(), any())).thenReturn(Future.successful(mock[Cache]))

    when(controller.landingService.setAltCorrespondenceAddress(any(), any[String])).thenReturn(Future.successful(mock[Cache]))

    val completeATB = mock[BusinessDetails]

    def setUpMocksForDataExistsInSaveForLater(controller: LandingController, testData: Cache = mock[Cache]) = {
      when(controller.landingService.cacheMap(any[String])).thenReturn(Future.successful(Some(testData)))
    }

    //noinspection ScalaStyle
  }

  "show landing page without authorisation" in new Fixture {
    val result = controller.start()(FakeRequest().withSession())
    status(result) mustBe OK
  }

  "show the landing page when authorised, but 'redirect' = false" in new Fixture {
    val result = controller.start(false)(request)
    status(result) mustBe OK
  }

  "direct to the service when authorised" in new Fixture {
    val result = controller.start()(request)
    status(result) must be(SEE_OTHER)
  }

  "Landing Controller" when {

    "redirect to status page" when {
      "submission status is DeRegistered and responsible person is not complete" in new FixtureNoAmlsNumber {
        val inCompleteResponsiblePeople: ResponsiblePerson = completeResponsiblePerson.copy(dateOfBirth = None)

        val cache: Cache = mock[Cache]
        val complete: BusinessMatching = mock[BusinessMatching]

        when(complete.isCompleteLanding) thenReturn true
        when(cache.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))
        when(cache.getEntry[BusinessDetails](BusinessDetails.key)).thenReturn(Some(completeATB))
        when(cache.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any())).thenReturn(Some(Seq(inCompleteResponsiblePeople)))
        when(cache.getEntry[SubscriptionResponse](SubscriptionResponse.key))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0)))))

        when(controller.landingService.cacheMap(any[String])).thenReturn(Future.successful(Some(cache)))
        when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any(), any()))
          .thenReturn(Future.successful((rejectedStatusGen.sample.get, None)))

        val result = controller.get()(request)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.routes.StatusController.get().url)
      }
    }

    "redirect to login event page" when {
      "redress scheme is invalid" in new FixtureNoAmlsNumber {

        val completeRedressScheme = Json.obj(
          "redressScheme" -> "ombudsmanServices",
          "redressSchemeDetail" -> "null"
        )

        val completeData = completeRedressScheme

        val eabOmbudsmanServices = Eab(completeData)

        val cache: Cache = mock[Cache]
        val complete: BusinessMatching = mock[BusinessMatching]

        when(complete.isCompleteLanding) thenReturn true
        when(cache.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))
        when(cache.getEntry[BusinessDetails](BusinessDetails.key)).thenReturn(Some(completeATB))
        when(cache.getEntry[Eab](meq(Eab.key))(any())).thenReturn(Some(eabOmbudsmanServices))
        when(cache.getEntry[SubscriptionResponse](SubscriptionResponse.key))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0)))))

        when(controller.landingService.cacheMap(any[String])).thenReturn(Future.successful(Some(cache)))
        when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any(), any()))
          .thenReturn(Future.successful((activeStatusGen.sample.get, None)))

        val data = Map(
          TradingPremises.key -> Json.toJson(TradingPremises()),
          BusinessMatching.key -> Json.toJson(BusinessMatching()),
          BusinessDetails.key -> Json.toJson(BusinessDetails()),
          Eab.key -> Json.toJson(Eab(completeRedressScheme)),
          SubscriptionResponse.key -> Json.toJson(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0))))
        )
        when(cache.id).thenReturn("cache-id")
        when(cache.data).thenReturn(data)

        val result = controller.get()(request)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.routes.LoginEventController.get.url)
      }
    }

    "an enrolment exists and" when {
      "there is data in S4L and" when {
        "the Save 4 Later data does not contain any sections" when {
          "data has not changed" should {
            "refresh from API5 and redirect to status controller" in new Fixture {
              setUpMocksForDataExistsInSaveForLater(controller, Cache("test", Map.empty))

              when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any()))
                .thenReturn(Future.successful(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0))))))

              when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any(), any()))
                .thenReturn(Future.successful((NotCompleted, None)))

              val result = controller.get()(request)

              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(controllers.routes.StatusController.get().url))
              verify(controller.landingService, atLeastOnce()).refreshCache(any[String](), any(), any())(any[HeaderCarrier], any[ExecutionContext], any())
            }
          }
        }

        "data has just been imported" should {

          def runImportTest(hasChanged: Boolean): Unit = new Fixture {
            val testCache = createTestCache(
              hasChanged = hasChanged,
              includesResponse = false,
              includeSubmissionStatus = true,
              includeDataImport = true)

            setUpMocksForDataExistsInSaveForLater(controller, testCache)

            when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any()))
              .thenReturn(Future.successful(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0))))))
            when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any(), any()))
              .thenReturn(Future.successful((NotCompleted, None)))

            val result = controller.get()(request)

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.routes.StatusController.get().url)
            verify(controller.landingService, never).refreshCache(any[String](), any(), any())(any(), any(), any())
          }

          "redirect to the status page without refreshing the cache" when {
            "hasChanged is false" in {
              runImportTest(hasChanged = false)
            }

            "hasChanged is true" in new Fixture {
              runImportTest(hasChanged = true)
            }
          }
        }

        "data has not changed and" when {
          "the user has just submitted" when {
            "there are no incomplete responsible people" should {
              "refresh from API5 and redirect to status controller" in new Fixture {
                val testCache = createTestCache(
                  hasChanged = true,
                  includesResponse = false,
                  includeSubmissionStatus = true)

                setUpMocksForDataExistsInSaveForLater(controller, testCache)

                when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any()))
                  .thenReturn(Future.successful(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0))))))

                when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any(), any()))
                  .thenReturn(Future.successful((NotCompleted, None)))

                val result = controller.get()(requestWithHeaders("test-context" -> "ESCS"))

                status(result) must be(SEE_OTHER)
                redirectLocation(result) must be(Some(controllers.routes.StatusController.get().url))

                verify(controller.landingService).refreshCache(any[String](), any(), any())(any[HeaderCarrier], any[ExecutionContext], any())
              }
            }

            "there is an invalid redress scheme" should {
              "refresh from API5 and redirect to login events controller" in new Fixture {

                val completeRedressSchemeOther = Json.obj(
                  "redressScheme" -> "other"
                )
                val eabOther = Eab(completeRedressSchemeOther, hasAccepted = true)

                val testCache = createTestCache(
                  hasChanged = true,
                  includesResponse = false,
                  includeSubmissionStatus = true)

                val updatedCache = Cache("test-cache-id", testCache.data + (Eab.key -> Json.toJson(eabOther)))

                setUpMocksForDataExistsInSaveForLater(controller, updatedCache)

                when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any()))
                  .thenReturn(Future.successful(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0))))))

                when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any(), any()))
                  .thenReturn(Future.successful((NotCompleted, None)))

                val result = controller.get()(requestWithHeaders(("test-context" -> "ESCS")))

                status(result) must be(SEE_OTHER)
                redirectLocation(result) must be(Some(controllers.routes.LoginEventController.get.url))

                verify(controller.landingService).refreshCache(any[String](), any(), any())(any[HeaderCarrier], any[ExecutionContext], any())
              }
            }
          }

          "the user has not just submitted" when {
            "there are no incomplete responsible people" should {
              "redirect to status controller without refreshing API5" in new Fixture {
                val testCache = createTestCache(
                  hasChanged = true,
                  includesResponse = false)

                when(controller.landingService.setAltCorrespondenceAddress(any(), any(), any(), any())(any(), any())).thenReturn(Future.successful(testCache))

                setUpMocksForDataExistsInSaveForLater(controller, testCache)

                when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any()))
                  .thenReturn(Future.successful(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0))))))

                when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any(), any()))
                  .thenReturn(Future.successful((NotCompleted, None)))

                val result = controller.get()(request)

                status(result) must be(SEE_OTHER)
                redirectLocation(result) must be(Some(controllers.routes.StatusController.get().url))

                verify(controller.landingService, never()).refreshCache(any[String](), any(), any())(any[HeaderCarrier], any[ExecutionContext], any())
              }
            }

            "there is an invalid redress scheme" should {
              "redirect to login events" in new Fixture {

                val completeRedressScheme = Json.obj(
                  "redressScheme" -> "ombudsmanServices",
                  "redressSchemeDetail" -> "null"
                )

                val completeData = completeRedressScheme

                val eabOmbudsmanServices = Eab(completeData)

                val testCache = createTestCache(
                  hasChanged = true,
                  includesResponse = false)

                val updatedCacheMap = Cache("test-cache-id", testCache.data + (Eab.key -> Json.toJson(eabOmbudsmanServices)))

                when(controller.landingService.setAltCorrespondenceAddress(any(), any(), any(), any())(any(), any())).thenReturn(Future.successful(updatedCacheMap))

                setUpMocksForDataExistsInSaveForLater(controller, updatedCacheMap)

                when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any()))
                  .thenReturn(Future.successful(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0))))))

                when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any(), any()))
                  .thenReturn(Future.successful((NotCompleted, None)))

                val result = controller.get()(request)

                status(result) must be(SEE_OTHER)
                redirectLocation(result) must be(Some(controllers.routes.LoginEventController.get.url))

                verify(controller.landingService, never()).refreshCache(any[String](), any(), any())(any[HeaderCarrier], any[ExecutionContext], any())
              }
            }
          }
        }

        "data has not changed" should {
          "refresh from API5 and redirect to status controller" in new Fixture {
            val testCache: Cache = createTestCache(hasChanged = false, includesResponse = false)
            setUpMocksForDataExistsInSaveForLater(controller, testCache)

            when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any()))
              .thenReturn(Future.successful(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0))))))

            when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any(), any()))
              .thenReturn(Future.successful((NotCompleted, None)))

            val result = controller.get()(request)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.routes.StatusController.get().url))
            verify(controller.landingService, atLeastOnce()).refreshCache(any[String](), any(), any())(any[HeaderCarrier], any[ExecutionContext], any())
          }

          "refresh from API5 and redirect to status controller with duplicate submission flag set" in new Fixture {
            when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any())).thenReturn(Future.successful(Some(SubscriptionResponse("", "", None, Some(true)))))

            val testCache = createTestCache(hasChanged = false, includesResponse = false)
            setUpMocksForDataExistsInSaveForLater(controller, testCache)
            when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any(), any()))
              .thenReturn(Future.successful((NotCompleted, None)))

            val result = controller.get()(request)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.routes.StatusController.get(true).url))
            verify(controller.landingService, atLeastOnce()).refreshCache(any[String](), any(), any())(any[HeaderCarrier], any[ExecutionContext], any())
          }

          "refresh from API5 and redirect to status controller when there is no TP or RP data" in new Fixture {
            val cacheMapOne: Cache = createTestCache(hasChanged = false, includesResponse = false, noTP = true, noRP = true)
            val testCache = cacheMapOne

            setUpMocksForDataExistsInSaveForLater(controller, testCache)

            val fixedCache = createTestCache(hasChanged = false, includesResponse = false)

            when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any()))
              .thenReturn(Future.successful(Some(SubscriptionResponse("", "", None))))

            when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any(), any()))
              .thenReturn(Future.successful((NotCompleted, None)))

            when(controller.cacheConnector.save[TradingPremises](any(), meq(TradingPremises.key), any())(any())).thenReturn(Future.successful(fixedCache))

            when(controller.cacheConnector.save[ResponsiblePerson](any(), meq(ResponsiblePerson.key), any())(any())).thenReturn(Future.successful(fixedCache))

            val result = controller.get()(request)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.routes.StatusController.get().url))
            verify(controller.landingService, atLeastOnce()).refreshCache(any[String](), any(), any())(any[HeaderCarrier], any[ExecutionContext], any())
          }
        }
      }


      "there is no data in S4L" should {
        "refresh from API5 and redirect to status controller" in new Fixture {
          when(controller.landingService.cacheMap(any[String])).thenReturn(Future.successful(None))
          when(controller.landingService.initialiseGetWithAmendments(any[String])(any())).thenReturn(Future.successful(None))

          when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any()))
            .thenReturn(Future.successful(Some(SubscriptionResponse("", "", None))))

          when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any(), any()))
            .thenReturn(Future.successful((NotCompleted, None)))

          val result = controller.get()(request)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.routes.StatusController.get().url))
          verify(controller.landingService, atLeastOnce()).refreshCache(any[String](), any(), any())(any[HeaderCarrier], any[ExecutionContext], any())
        }
      }
    }

    "an enrolment does not exist" when {
      "there is data in S4L " should {
        "not refresh API5 and redirect to status controller" in new FixtureNoAmlsNumber {

          val businessMatching = mock[BusinessMatching]
          val cache = mock[Cache]

          when(controller.landingService.cacheMap(any[String])) thenReturn Future.successful(Some(cache))
          when(businessMatching.isCompleteLanding) thenReturn true
          when(cache.getEntry[BusinessMatching](any())(any())).thenReturn(Some(businessMatching))
          when(cache.getEntry[BusinessDetails](BusinessDetails.key)).thenReturn(Some(completeATB))
          when(cache.getEntry[Eab](meq(Eab.key))(any())).thenReturn(None)
          when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any(), any()))
            .thenReturn(Future.successful((NotCompleted, None)))

          val result = controller.get()(request)

          verify(controller.landingService, never()).refreshCache(any[String](), any(), any())(any[HeaderCarrier], any[ExecutionContext], any())
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.routes.StatusController.get().url))
        }
      }

      "there is no data in S4L" when {
        "there is data in keystore " should {
          "copy keystore data to S4L and redirect to business type controler" in new FixtureNoAmlsNumber {
            when(controller.landingService.cacheMap(any[String])).thenReturn(Future.successful(None))


            val reviewDetails = ReviewDetails(
              "Business Name",
              None,
              Address("Line1", Some("Line2"), None, None, Some("AA11AA"), Country("United Kingdom", "UK")),
              "testSafeId")

            when(controller.landingService.reviewDetails(any[HeaderCarrier], any[ExecutionContext]))
              .thenReturn(Future.successful(Some(reviewDetails)))

            when(controller.landingService.updateReviewDetails(any(), any())).thenReturn(Future.successful(mock[Cache]))

            val result = controller.get()(request)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.businessmatching.routes.BusinessTypeController.get().url))

            Mockito.verify(controller.landingService, times(1)).updateReviewDetails(any[ReviewDetails], any[String])
          }
        }

        "there is no data in keystore" should {
          "redirect to business customer" in new FixtureNoAmlsNumber {
            when(controller.landingService.cacheMap(any[String])).thenReturn(Future.successful(None))
            when(controller.landingService.reviewDetails(any[HeaderCarrier], any[ExecutionContext]))
              .thenReturn(Future.successful(None))

            val result = controller.get()(request)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("http://localhost:9923/business-customer/amls"))
          }
        }
      }
    }
  }
}
