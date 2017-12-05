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
import generators.tradingpremises.TradingPremisesGenerator

import scala.concurrent.Future


class TradingPremisesAddControllerSpec extends GenericTestHelper with PropertyChecks with TradingPremisesGenerator{

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
        .thenReturn(Some(Seq(tradingPremisesGen.sample.get)))

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
