package controllers.tradingpremises

import connectors.DataCacheConnector
import models.businessmatching._
import models.tradingpremises.TradingPremises
import org.scalatest.prop.PropertyChecks
import utils.GenericTestHelper
import utils.AuthorisedFixture
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito._
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future


class TradingPremisesAddControllerSpec extends GenericTestHelper with PropertyChecks {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new TradingPremisesAddController (mock[DataCacheConnector], self.authConnector)
  }

  "TradingPremisesAddController" should {
    val emptyCache = CacheMap("", Map.empty)

    "load What You Need successfully when displayGuidance is true" in new Fixture {

      val BusinessActivitiesModel = BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
      val mockCacheMap = mock[CacheMap]

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(BusinessActivitiesModel))))

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.get(true)(request)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.WhatYouNeedController.get(1).url))
    }

    "load Where Are Trading Premises page successfully when user selects option other then MSB in business matching page" in new Fixture {

      val BusinessActivitiesModel = BusinessActivities(Set(TrustAndCompanyServices, TelephonePaymentService))
      val mockCacheMap = mock[CacheMap]


      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(TradingPremises(), TradingPremises())))

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(BusinessActivitiesModel))))

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.fetch[TradingPremises](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.get(false)(request)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1, false).url))
    }

    "load confirm trading premises address page successfully when user selects option other then MSB in business matching page" in new Fixture {

      val BusinessActivitiesModel = BusinessActivities(Set(TrustAndCompanyServices, TelephonePaymentService))
      val mockCacheMap = mock[CacheMap]


      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(TradingPremises())))

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(BusinessActivitiesModel))))

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.fetch[TradingPremises](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.get(false)(request)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.ConfirmAddressController.get(1).url))
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

      when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.get(false)(request)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.RegisteringAgentPremisesController.get(1, false).url))
    }
  }
}
