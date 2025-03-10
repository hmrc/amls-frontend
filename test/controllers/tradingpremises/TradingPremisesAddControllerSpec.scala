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

package controllers.tradingpremises

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import generators.tradingpremises.TradingPremisesGenerator
import models.businessmatching._
import models.businessmatching.BusinessActivity._
import models.tradingpremises.TradingPremises
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.test.Helpers._
import services.cache.Cache
import utils.AmlsSpec

import scala.concurrent.Future

class TradingPremisesAddControllerSpec extends AmlsSpec with ScalaCheckPropertyChecks with TradingPremisesGenerator {

  trait Fixture {
    self =>
    val request = addToken(authRequest)

    val controller = new TradingPremisesAddController(
      mock[DataCacheConnector],
      SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      error = errorView
    )
  }

  "TradingPremisesAddController" should {
    val emptyCache = Cache.empty

    "load What You Need successfully when displayGuidance is true" in new Fixture {

      val BusinessActivitiesModel =
        BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
      val mockCacheMap            = mock[Cache]

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(BusinessActivitiesModel))))

      when(controller.dataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any(), any())(any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.get(true)(request)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.WhatYouNeedController.get(1).url))
    }

    "load Where Are Trading Premises page successfully when user selects option other then MSB in business matching page" in new Fixture {

      val BusinessActivitiesModel = BusinessActivities(Set(TrustAndCompanyServices, TelephonePaymentService))
      val mockCacheMap            = mock[Cache]

      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(TradingPremises(), TradingPremises())))

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(BusinessActivitiesModel))))

      when(controller.dataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.fetch[TradingPremises](any(), any())(any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.get(false)(request)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1, false).url))
    }

    "load confirm trading premises address page successfully when user selects option other then MSB in business matching page" in new Fixture {

      val BusinessActivitiesModel = BusinessActivities(Set(TrustAndCompanyServices, TelephonePaymentService))
      val mockCacheMap            = mock[Cache]

      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(tradingPremisesGen.sample.get)))

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(BusinessActivitiesModel))))

      when(controller.dataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.fetch[TradingPremises](any(), any())(any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.get(false)(request)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.ConfirmAddressController.get(1).url))
    }

    "load Registering Agent Premises page successfully when user selects MSB in business matching page" in new Fixture {

      val BusinessActivitiesModel =
        BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
      val mockCacheMap            = mock[Cache]

      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(BusinessActivitiesModel))))

      when(controller.dataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.fetch[TradingPremises](any(), any())(any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.get(false)(request)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.RegisteringAgentPremisesController.get(1, false).url))
    }
  }
}
