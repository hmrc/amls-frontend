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


  }
}
