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


import generators.businessmatching.BusinessMatchingGenerator
import generators.tradingpremises.TradingPremisesGenerator
import models.businessmatching.{BusinessActivities, BusinessMatching, HighValueDealing, MoneyServiceBusiness}
import models.businessmatching.updateservice.{ServiceChangeRegister, TradingPremisesActivities}
import models.flowmanagement.AddServiceFlowModel
import models.status.SubmissionDecisionApproved
import models.tradingpremises.{TradingPremises, WhatDoesYourBusinessDo}
import org.scalacheck.Gen
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import services.TradingPremisesService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

import scala.concurrent.ExecutionContext

class UpdateServicesSummaryControllerSpec extends GenericTestHelper
  with MockitoSugar
  with TradingPremisesGenerator
  with BusinessMatchingGenerator {

  sealed trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>
    val request = addToken(authRequest)

    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val authContext: AuthContext = mock[AuthContext]
    implicit val ec: ExecutionContext = mock[ExecutionContext]

    mockCacheFetch(Some(AddServiceFlowModel(Some(HighValueDealing))))
    mockApplicationStatus(SubmissionDecisionApproved)

    val mockTradingPremisesService = mock[TradingPremisesService]

    val controller = new UpdateServicesSummaryController(
      self.authConnector,
      mockCacheConnector,
      mockTradingPremisesService
    )
  }

  "Get" must {
    "return OK with update_service_summary view" in new Fixture {

      val result = controller.get()(request)
      status(result) must be(OK)

      contentAsString(result) must include(Messages("businessmatching.updateservice.selectactivities.title"))

      contentAsString(result) must include(Messages("button.checkyouranswers.acceptandcomplete"))
    }
  }

  "post is called" must {
    "respond with OK and redirect to the 'do you want to add more activities' page " +
      "if the user clicks continue and there are available Activities to select" in new Fixture {

      //noinspection ScalaStyle
      val tradingPremises = Gen.listOfN(5, tradingPremisesGen).sample.get.toSeq

      val modifiedTradingPremises = tradingPremises map {_.copy(
        whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing)))
      )}

      val flowModel = AddServiceFlowModel(
          Some(HighValueDealing),
          Some(true),
          Some(TradingPremisesActivities(Set(0)))
      )

      val businessMatchingModel = businessMatchingGen.sample.get.copy(
          activities = Some(BusinessActivities(Set(MoneyServiceBusiness)))
      )

      when {
        controller.tradingPremisesService.addBusinessActivtiesToTradingPremises(eqTo(Seq(0)), eqTo(tradingPremises), eqTo(HighValueDealing), eqTo(false))
      } thenReturn modifiedTradingPremises

      mockCacheFetch[Seq[TradingPremises]](Some(tradingPremises), Some(TradingPremises.key))
      mockCacheFetch[AddServiceFlowModel](Some(flowModel), Some(AddServiceFlowModel.key))

      mockCacheSave(modifiedTradingPremises, Some(TradingPremises.key))
      mockCacheSave(flowModel.copy(hasAccepted = true), Some(AddServiceFlowModel.key))

      mockCacheUpdate[ServiceChangeRegister](Some(ServiceChangeRegister.key), ServiceChangeRegister())
      mockCacheUpdate[BusinessMatching](Some(BusinessMatching.key), businessMatchingModel)

      val result = controller.post()(request)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.businessmatching.updateservice.add.routes.AddMoreActivitiesController.get().url))
    }

  }
}

