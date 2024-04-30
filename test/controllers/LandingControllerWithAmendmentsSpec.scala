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
import connectors.cache.CacheConverter
import connectors.cache.Conversions.DelegateCacheMap
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
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{BodyParsers, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.cache.Cache
import services.{AuthEnrolmentsService, LandingService, StatusService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter
import utils.{AmlsSpec, AuthorisedFixture}
import views.html.Start

import scala.concurrent.{ExecutionContext, Future}

class LandingControllerWithAmendmentsSpec extends AmlsSpec with MockitoSugar with Matchers with StatusGenerator
  with ResponsiblePeopleValues with CacheMapValues {

  val businessCustomerUrl = "TestUrl"

  lazy val headerCarrierForPartialsConverter = app.injector.instanceOf[HeaderCarrierForPartialsConverter]

  trait Fixture {
    self =>

    val request = addToken(authRequest)
    val config = mock[ApplicationConfig]
    val mockCacheConverter = mock[CacheConverter]
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
      applicationCrypto = applicationCrypto,
      cacheConverter = mockCacheConverter)

    when(controller.landingService.refreshCache(any(), any[String](), any())(any(), any(), any())).thenReturn(Future.successful(mock[CacheMap]))

    when(controller.landingService.setAltCorrespondenceAddress(any(), any[String])(any(), any())).thenReturn(Future.successful(mock[CacheMap]))

    val completeATB = mock[BusinessDetails]

    def setUpMocksForDataExistsInSaveForLater(controller: LandingController, testData: CacheMap = mock[CacheMap]) = {
      when(controller.landingService.cacheMap(any[String])(any(), any())).thenReturn(Future.successful(Some(testData)))
    }

    //noinspection ScalaStyle
  }

  trait FixtureNoAmlsNumber extends AuthorisedFixture {
    self =>

    val request = addToken(authRequest)
    val config = mock[ApplicationConfig]
    val mockCacheConverter = mock[CacheConverter]
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
      applicationCrypto = applicationCrypto,
      cacheConverter = mockCacheConverter)

    when(controller.landingService.refreshCache(any(), any[String](), any())(any(), any(), any())).thenReturn(Future.successful(mock[CacheMap]))

    when(controller.landingService.setAltCorrespondenceAddress(any(), any[String])(any(), any())).thenReturn(Future.successful(mock[CacheMap]))

    val completeATB = mock[BusinessDetails]

    def setUpMocksForDataExistsInSaveForLater(controller: LandingController, testData: CacheMap = mock[CacheMap]) = {
      when(controller.landingService.cacheMap(any[String])(any(), any())).thenReturn(Future.successful(Some(testData)))
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

        val cacheMap: CacheMap = mock[CacheMap]
        val complete: BusinessMatching = mock[BusinessMatching]

        when(complete.isCompleteLanding) thenReturn true
        when(cacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))
        when(cacheMap.getEntry[BusinessDetails](BusinessDetails.key)).thenReturn(Some(completeATB))
        when(cacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any())).thenReturn(Some(Seq(inCompleteResponsiblePeople)))
        when(cacheMap.getEntry[SubscriptionResponse](SubscriptionResponse.key))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0)))))

        when(controller.landingService.cacheMap(any[String])(any(), any())) thenReturn Future.successful(Some(cacheMap))
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

        val cacheMap: CacheMap = mock[CacheMap]
        val complete: BusinessMatching = mock[BusinessMatching]

        when(complete.isCompleteLanding) thenReturn true
        when(cacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))
        when(cacheMap.getEntry[BusinessDetails](BusinessDetails.key)).thenReturn(Some(completeATB))
        when(cacheMap.getEntry[Eab](meq(Eab.key))(any())).thenReturn(Some(eabOmbudsmanServices))
        when(cacheMap.getEntry[SubscriptionResponse](SubscriptionResponse.key))
          .thenReturn(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0)))))

        when(controller.landingService.cacheMap(any[String])(any(), any())) thenReturn Future.successful(Some(cacheMap))
        when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any(), any()))
          .thenReturn(Future.successful((activeStatusGen.sample.get, None)))

        val data = Map(
          TradingPremises.key -> Json.toJson(TradingPremises()),
          BusinessMatching.key -> Json.toJson(BusinessMatching()),
          BusinessDetails.key -> Json.toJson(BusinessDetails()),
          Eab.key -> Json.toJson(Eab(completeRedressScheme)),
          SubscriptionResponse.key -> Json.toJson(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0))))
        )
        when(cacheMap.id).thenReturn("cache-id")
        when(cacheMap.data).thenReturn(data)

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
              setUpMocksForDataExistsInSaveForLater(controller, CacheMap("test", Map.empty))

              when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any()))
                .thenReturn(Future.successful(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0))))))

              when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any(), any()))
                .thenReturn(Future.successful((NotCompleted, None)))

              val delegateCacheMap = new DelegateCacheMap(Cache("empty-test-cache", Map.empty[String, JsValue]))
              when(mockCacheConverter.toCacheMap(any())).thenReturn(delegateCacheMap)

              val result = controller.get()(request)

              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(controllers.routes.StatusController.get().url))
              verify(controller.landingService, atLeastOnce()).refreshCache(any[String](), any(), any())(any[HeaderCarrier], any[ExecutionContext], any())
            }
          }
        }

        "data has just been imported" should {

          def runImportTest(hasChanged: Boolean): Unit = new Fixture {
            val testCacheMap = createTestCache(
              hasChanged = hasChanged,
              includesResponse = false,
              includeSubmissionStatus = true,
              includeDataImport = true)

            setUpMocksForDataExistsInSaveForLater(controller, testCacheMap)

            when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any()))
              .thenReturn(Future.successful(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0))))))
            when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any(), any()))
              .thenReturn(Future.successful((NotCompleted, None)))

            val result = controller.get()(request)

            val delegateCacheMap = new DelegateCacheMap(Cache(testCacheMap.id, testCacheMap.data))
            when(mockCacheConverter.toCacheMap(any())).thenReturn(delegateCacheMap)

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
                val testCacheMap = createTestCache(
                  hasChanged = true,
                  includesResponse = false,
                  includeSubmissionStatus = true)

                setUpMocksForDataExistsInSaveForLater(controller, testCacheMap)

                when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any()))
                  .thenReturn(Future.successful(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0))))))

                when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any(), any()))
                  .thenReturn(Future.successful((NotCompleted, None)))

                val updatedTestCacheMap = testCacheMap.data + (ResponsiblePerson.key -> Json.toJson(Seq.empty[ResponsiblePerson]))
                val delegateCacheMap = new DelegateCacheMap(Cache(testCacheMap.id, updatedTestCacheMap))
                when(mockCacheConverter.toCacheMap(any())).thenReturn(delegateCacheMap)

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

                val testCacheMap = createTestCache(
                  hasChanged = true,
                  includesResponse = false,
                  includeSubmissionStatus = true)

                val updatedCacheMap = CacheMap("test-cache-id", testCacheMap.data + (Eab.key -> Json.toJson(eabOther)))

                setUpMocksForDataExistsInSaveForLater(controller, updatedCacheMap)

                when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any()))
                  .thenReturn(Future.successful(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0))))))

                when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any(), any()))
                  .thenReturn(Future.successful((NotCompleted, None)))

                val updatedTestCacheMap = testCacheMap.data + (Eab.key -> Json.toJson(eabOther))
                val delegateCacheMap = new DelegateCacheMap(Cache(testCacheMap.id, updatedTestCacheMap))
                when(mockCacheConverter.toCacheMap(any())).thenReturn(delegateCacheMap)

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
                val testCacheMap = createTestCache(
                  hasChanged = true,
                  includesResponse = false)

                when {
                  controller.landingService.setAltCorrespondenceAddress(any(), any(), any(), any())(any(), any())
                } thenReturn Future.successful(testCacheMap)

                setUpMocksForDataExistsInSaveForLater(controller, testCacheMap)

                when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any()))
                  .thenReturn(Future.successful(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0))))))

                when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any(), any()))
                  .thenReturn(Future.successful((NotCompleted, None)))

                val updatedTestCacheMap = testCacheMap.data + (ResponsiblePerson.key -> Json.toJson(Seq.empty[ResponsiblePerson]))
                val delegateCacheMap = new DelegateCacheMap(Cache(testCacheMap.id, updatedTestCacheMap))
                when(mockCacheConverter.toCacheMap(any())).thenReturn(delegateCacheMap)

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

                val testCacheMap = createTestCache(
                  hasChanged = true,
                  includesResponse = false)

                val updatedCacheMap = CacheMap("test-cache-id", testCacheMap.data + (Eab.key -> Json.toJson(eabOmbudsmanServices)))

                when {
                  controller.landingService.setAltCorrespondenceAddress(any(), any(), any(), any())(any(), any())
                } thenReturn Future.successful(updatedCacheMap)

                setUpMocksForDataExistsInSaveForLater(controller, updatedCacheMap)

                when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any()))
                  .thenReturn(Future.successful(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0))))))

                when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any(), any()))
                  .thenReturn(Future.successful((NotCompleted, None)))

                val updatedTestCacheMap = testCacheMap.data + (Eab.key -> Json.toJson(eabOmbudsmanServices))
                val delegateCacheMap = new DelegateCacheMap(Cache(testCacheMap.id, updatedTestCacheMap))
                when(mockCacheConverter.toCacheMap(any())).thenReturn(delegateCacheMap)

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
            val testCache: CacheMap = createTestCache(hasChanged = false, includesResponse = false)
            setUpMocksForDataExistsInSaveForLater(controller, testCache)

            when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any()))
              .thenReturn(Future.successful(Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0))))))

            when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any(), any()))
              .thenReturn(Future.successful((NotCompleted, None)))

            val delegateCacheMap = new DelegateCacheMap(Cache("test-cache", testCache.data))
            when(mockCacheConverter.toCacheMap(any())).thenReturn(delegateCacheMap)

            val result = controller.get()(request)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.routes.StatusController.get().url))
            verify(controller.landingService, atLeastOnce()).refreshCache(any[String](), any(), any())(any[HeaderCarrier], any[ExecutionContext], any())
          }

          "refresh from API5 and redirect to status controller with duplicate submission flag set" in new Fixture {
            when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any()))
              .thenReturn(Future.successful(Some(SubscriptionResponse("", "", None, Some(true)))))

            val testCacheMap = createTestCache(hasChanged = false, includesResponse = false)
            setUpMocksForDataExistsInSaveForLater(controller, testCacheMap)
            when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any(), any()))
              .thenReturn(Future.successful((NotCompleted, None)))

            val updatedCacheMapData: Map[String, JsValue] = testCacheMap.data.-(ResponsiblePerson.key)
            val delegateCacheMap = new DelegateCacheMap(Cache("test-cache", updatedCacheMapData))
            when(mockCacheConverter.toCacheMap(any())).thenReturn(delegateCacheMap)

            val result = controller.get()(request)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.routes.StatusController.get(true).url))
            verify(controller.landingService, atLeastOnce()).refreshCache(any[String](), any(), any())(any[HeaderCarrier], any[ExecutionContext], any())
          }

          "refresh from API5 and redirect to status controller when there is no TP or RP data" in new Fixture {
            val cacheMapOne: CacheMap = createTestCache(hasChanged = false, includesResponse = false, noTP = true, noRP = true)
            val testCacheMap = cacheMapOne

            setUpMocksForDataExistsInSaveForLater(controller, testCacheMap)

            val fixedCacheMap = createTestCache(hasChanged = false, includesResponse = false)

            when(controller.cacheConnector.fetch[SubscriptionResponse](any(), any())(any()))
              .thenReturn(Future.successful(Some(SubscriptionResponse("", "", None))))

            when(controller.statusService.getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any(), any()))
              .thenReturn(Future.successful((NotCompleted, None)))

            val mergedCache = cacheMapOne.data
              .+(SubscriptionResponse.key -> Json.toJson(SubscriptionResponse("", "", None)))
              .+(ResponsiblePerson.key -> Json.toJson(None))

            val delegateCacheMap = new DelegateCacheMap(Cache("test-cache", mergedCache))
            when(mockCacheConverter.toCacheMap(any())).thenReturn(delegateCacheMap)

            when {
              controller.cacheConnector.save[TradingPremises](any(), meq(TradingPremises.key), any())(any())
            } thenReturn Future.successful(fixedCacheMap)

            when {
              controller.cacheConnector.save[ResponsiblePerson](any(), meq(ResponsiblePerson.key), any())(any())
            } thenReturn Future.successful(fixedCacheMap)

            val result = controller.get()(request)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.routes.StatusController.get().url))
            verify(controller.landingService, atLeastOnce()).refreshCache(any[String](), any(), any())(any[HeaderCarrier], any[ExecutionContext], any())
          }
        }
      }


      "there is no data in S4L" should {
        "refresh from API5 and redirect to status controller" in new Fixture {
          when(controller.landingService.cacheMap(any[String])(any(), any())).thenReturn(Future.successful(None))

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
          val cacheMap = mock[CacheMap]

          when(controller.landingService.cacheMap(any[String])(any(), any())) thenReturn Future.successful(Some(cacheMap))
          when(businessMatching.isCompleteLanding) thenReturn true
          when(cacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(businessMatching))
          when(cacheMap.getEntry[BusinessDetails](BusinessDetails.key)).thenReturn(Some(completeATB))
          when(cacheMap.getEntry[Eab](meq(Eab.key))(any())).thenReturn(None)
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
            when(controller.landingService.cacheMap(any[String])(any(), any())).thenReturn(Future.successful(None))


            val reviewDetails = ReviewDetails(
              "Business Name",
              None,
              Address("Line1", Some("Line2"), None, None, Some("AA11AA"), Country("United Kingdom", "UK")),
              "testSafeId")

            when(controller.landingService.reviewDetails(any[HeaderCarrier], any[ExecutionContext], any[Request[_]]))
              .thenReturn(Future.successful(Some(reviewDetails)))

            when(controller.landingService.updateReviewDetails(any(), any())(any(), any())).thenReturn(Future.successful(mock[CacheMap]))

            val result = controller.get()(request)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.businessmatching.routes.BusinessTypeController.get.url))

            Mockito.verify(controller.landingService, times(1))
              .updateReviewDetails(any[ReviewDetails], any[String])(any[HeaderCarrier], any[ExecutionContext])
          }
        }

        "there is no data in keystore" should {
          "redirect to business customer" in new FixtureNoAmlsNumber {
            when(controller.landingService.cacheMap(any[String])(any(), any())).thenReturn(Future.successful(None))
            when(controller.landingService.reviewDetails(any[HeaderCarrier], any[ExecutionContext], any[Request[_]]))
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
