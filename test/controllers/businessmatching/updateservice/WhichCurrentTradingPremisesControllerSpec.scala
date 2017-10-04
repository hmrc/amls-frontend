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

package controllers.businessmatching.updateservice

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import generators.tradingpremises.TradingPremisesGenerator
import models.businessmatching.{AccountancyServices, BusinessActivity, HighValueDealing, MoneyServiceBusiness}
import models.tradingpremises.{TradingPremises, WhatDoesYourBusinessDo}
import org.scalatest.MustMatchers
import org.scalatest.mock.MockitoSugar
import play.api.inject.guice.GuiceInjectorBuilder
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}
import play.api.test.Helpers._
import play.api.inject.bind
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.mockito.ArgumentCaptor
import play.api.i18n.Messages
import services.businessmatching.BusinessMatchingService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WhichCurrentTradingPremisesControllerSpec extends GenericTestHelper
  with TradingPremisesGenerator
  with MustMatchers
  with MockitoSugar {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>
    implicit val request = addToken(authRequest)

    val bmService = mock[BusinessMatchingService]

    val injector = new GuiceInjectorBuilder()
      .bindings(bind[AuthConnector].to(self.authConnector))
      .bindings(bind[DataCacheConnector].to(mockCacheConnector))
      .bindings(bind[BusinessMatchingService].to(bmService))
      .build()

    lazy val controller = injector.instanceOf[WhichCurrentTradingPremisesController]

    mockCacheFetch[Seq[TradingPremises]](Some(Seq(TradingPremises())))

    when {
      bmService.getSubmittedBusinessActivities(any(), any(), any())
    } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(AccountancyServices))

  }

  "get" when {
    "called" must {
      "return the correct view" in new Fixture {
        val result = controller.get()(request)

        status(result) mustBe OK

        contentAsString(result) must include(Messages("businessmatching.updateservice.whichtradingpremises.header", AccountancyServices.getMessage))
      }
    }
  }

  "post" when {
    "called" must {
      "produce a validation error if no trading premises were selected" in new Fixture {
        val result = controller.post()(request.withFormUrlEncodedBody())

        status(result) mustBe BAD_REQUEST
      }

      "update the trading premises with the selected services" in new Fixture {
        val models = Seq(
          tradingPremisesWithActivitiesGen(AccountancyServices, HighValueDealing).sample.get,
          tradingPremisesWithActivitiesGen(AccountancyServices, HighValueDealing).sample.get,
          tradingPremisesWithActivitiesGen(MoneyServiceBusiness).sample.get
        )

        mockCacheFetch[Seq[TradingPremises]](Some(models))
        mockCacheSave[Seq[TradingPremises]]

        val form = Seq(
          "tradingPremises[]" -> "0",
          "tradingPremises[]" -> "2"
        )

        val result = controller.post()(request.withFormUrlEncodedBody(form:_*))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get().url)

        val captor = ArgumentCaptor.forClass(classOf[Seq[TradingPremises]])
        verify(mockCacheConnector).save[Seq[TradingPremises]](any(), captor.capture())(any(), any(), any())

        captor.getValue.lift(0).get.whatDoesYourBusinessDoAtThisAddress mustBe Some(WhatDoesYourBusinessDo(Set(AccountancyServices, HighValueDealing), None))
        captor.getValue.lift(1).get.whatDoesYourBusinessDoAtThisAddress mustBe Some(WhatDoesYourBusinessDo(Set(HighValueDealing), None))
        captor.getValue.lift(2).get.whatDoesYourBusinessDoAtThisAddress mustBe Some(WhatDoesYourBusinessDo(Set(AccountancyServices, MoneyServiceBusiness), None))

        captor.getValue.head.isComplete mustBe true
        captor.getValue.head.hasChanged mustBe true
      }

      "mark the trading premises as incomplete if there are no activities left" in new Fixture {
        val models = Seq(
          tradingPremisesWithActivitiesGen(AccountancyServices).sample.get,
          tradingPremisesWithActivitiesGen(AccountancyServices, HighValueDealing).sample.get
        )

        mockCacheFetch[Seq[TradingPremises]](Some(models))
        mockCacheSave[Seq[TradingPremises]]

        val form = "tradingPremises[]" -> "1"
        val result = controller.post()(request.withFormUrlEncodedBody(form))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get().url)

        val captor = ArgumentCaptor.forClass(classOf[Seq[TradingPremises]])
        verify(mockCacheConnector).save[Seq[TradingPremises]](any(), captor.capture())(any(), any(), any())

        captor.getValue.lift(0).get.whatDoesYourBusinessDoAtThisAddress mustBe Some(WhatDoesYourBusinessDo(Set(), None))
        captor.getValue.lift(1).get.whatDoesYourBusinessDoAtThisAddress mustBe Some(WhatDoesYourBusinessDo(Set(AccountancyServices, HighValueDealing), None))

        captor.getValue.head.isComplete mustBe false
        captor.getValue.head.hasChanged mustBe true
      }
    }
  }
}
