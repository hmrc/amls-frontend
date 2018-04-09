/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.businessmatching.updateservice.add


import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import generators.businessmatching.BusinessMatchingGenerator
import generators.tradingpremises.TradingPremisesGenerator
import models.businessmatching._
import models.businessmatching.updateservice.{ServiceChangeRegister, TradingPremisesActivities}
import models.flowmanagement.AddServiceFlowModel
import models.status.SubmissionDecisionApproved
import models.tradingpremises.{TradingPremises, WhatDoesYourBusinessDo}
import org.mockito.Matchers.{eq => eqTo}
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import services.flowmanagement.Router
import services.{StatusService, TradingPremisesService}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

import scala.concurrent.ExecutionContext

class UpdateServicesSummaryControllerSpec extends GenericTestHelper
  with MockitoSugar
  with TradingPremisesGenerator
  with BusinessMatchingGenerator {

  sealed trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val request = addToken(authRequest)

    implicit val authContext: AuthContext = mockAuthContext
    implicit val ec: ExecutionContext = mockExecutionContext

    val mockBusinessMatchingService = mock[BusinessMatchingService]
    val mockTradingPremisesService = mock[TradingPremisesService]
    val mockUpdateServicesSummaryControllerHelper = mock[UpdateServicesSummaryControllerHelper]

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[BusinessMatchingService].to(mockBusinessMatchingService))
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))
      .overrides(bind[StatusService].to(mockStatusService))
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[TradingPremisesService].to(mockTradingPremisesService))
      .overrides(bind[UpdateServicesSummaryControllerHelper].to(mockUpdateServicesSummaryControllerHelper))
      .overrides(bind[Router[AddServiceFlowModel]].to(createRouter[AddServiceFlowModel]))
      .build()

    val controller = app.injector.instanceOf[UpdateServicesSummaryController]

    val flowModel = AddServiceFlowModel(
      Some(HighValueDealing),
      Some(true),
      Some(TradingPremisesActivities(Set(0)))
    )
    mockCacheFetch(Some(flowModel))
    mockApplicationStatus(SubmissionDecisionApproved)


  }

  "UpdateServicesSummaryController" when {


    "get is called" must {
      "return OK with update_service_summary view" in new Fixture {

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("title.cya"))

        contentAsString(result) must include(Messages("button.checkyouranswers.acceptandcomplete"))
      }
    }

    "post is called" must {
      "respond with OK and redirect to the 'do you want to add more activities' page " +
        "if the user clicks continue and there are available Activities to select" in new Fixture {

        //noinspection ScalaStyle

        val tradingPremises: Seq[TradingPremises] = Gen.listOfN(5, tradingPremisesGen).sample.get

        val modifiedTradingPremises = tradingPremises map {
          _.copy(
            whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing)))
          )
        }

        val businessMatchingModel = businessMatchingGen.sample.get.copy(
          activities = Some(BusinessActivities(Set(BillPaymentServices)))
        )

        override val mockCacheMap = mock[CacheMap]

        val serviceChangeRegister:ServiceChangeRegister = ServiceChangeRegister(
          Some(Set(BillPaymentServices))
        )

        when {
          controller.updateServicesSummaryControllerHelper.updateTradingPremises(eqTo(flowModel))(any(), any())

        } thenReturn OptionT.fromOption[Future](Some(modifiedTradingPremises))

        when {
          controller.updateServicesSummaryControllerHelper.updateBusinessMatching(eqTo(HighValueDealing))(any(), any())
        } thenReturn Future.successful(Some(businessMatchingModel))

        when {
          controller.updateServicesSummaryControllerHelper.updateServicesRegister(eqTo(HighValueDealing))(any(), any())
        } thenReturn Future.successful(Some(serviceChangeRegister))

        when {
          controller.tradingPremisesService.addBusinessActivtiesToTradingPremises(eqTo(Seq(0)), eqTo(tradingPremises), eqTo(HighValueDealing), eqTo(false))
        } thenReturn modifiedTradingPremises

        when {
          controller.updateServicesSummaryControllerHelper.updateHasAcceptedFlag(eqTo(flowModel))(any(), any())
        } thenReturn OptionT.fromOption[Future](Some(mockCacheMap))
//
//        mockCacheFetch[Seq[TradingPremises]](Some(tradingPremises), Some(TradingPremises.key))
//        mockCacheFetch[AddServiceFlowModel](Some(flowModel), Some(AddServiceFlowModel.key))
//
//        mockCacheSave(modifiedTradingPremises, Some(TradingPremises.key))
//        mockCacheSave(flowModel.copy(hasAccepted = true), Some(AddServiceFlowModel.key))
//
//        mockCacheUpdate[ServiceChangeRegister](Some(ServiceChangeRegister.key), ServiceChangeRegister())
//        mockCacheUpdate[BusinessMatching](Some(BusinessMatching.key), businessMatchingModel)

        val result = controller.post()(request)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.businessmatching.updateservice.add.routes.AddMoreActivitiesController.get().url))
      }

    }
  }
}

