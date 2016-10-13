package controllers.tradingpremises

import connectors.DataCacheConnector
import models.businessmatching._
import models.tradingpremises.TradingPremises
import org.scalatest.prop.PropertyChecks
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import utils.AuthorisedFixture
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito._
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future


class TradingPremisesAddControllerSpec extends PlaySpec with OneAppPerSuite with PropertyChecks {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new TradingPremisesAddController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "TradingPremisesAddController" should {

    "load What You Need successfully when displayGuidance is true" in new Fixture {

      val BusinessActivitiesModel = BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
      val mockCacheMap = mock[CacheMap]

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(BusinessActivitiesModel))))

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.fetch[TradingPremises](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.get(true)(request)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.WhatYouNeedController.get(1).url))
    }

    "load Where Are Trading Premises page successfully when user selects option other then MSB in business matching page" in new Fixture {

      val BusinessActivitiesModel = BusinessActivities(Set(TrustAndCompanyServices, TelephonePaymentService))
      val mockCacheMap = mock[CacheMap]

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(BusinessActivitiesModel))))

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.fetch[TradingPremises](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.get(false)(request)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1, false).url))
    }

    "load Registering Agent Premises page successfully when user selects MSB in business matching page" in new Fixture {

      val BusinessActivitiesModel = BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
      val mockCacheMap = mock[CacheMap]

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(BusinessActivitiesModel))))

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.fetch[TradingPremises](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.get(false)(request)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.RegisteringAgentPremisesController.get(1, false).url))
    }
  }
}
