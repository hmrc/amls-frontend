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
import controllers.businessmatching.updateservice.AddBusinessTypeHelper
import generators.businessmatching.BusinessMatchingGenerator
import generators.tradingpremises.TradingPremisesGenerator
import models.businessmatching._
import models.businessmatching.updateservice.{ServiceChangeRegister, TradingPremisesActivities}
import models.flowmanagement.{AddBusinessTypeFlowModel, AddBusinessTypeSummaryPageId}
import models.responsiblepeople.ResponsiblePerson
import models.status.SubmissionDecisionApproved
import models.supervision.Supervision
import models.tradingpremises.{TradingPremises, WhatDoesYourBusinessDo}
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.TradingPremisesService
import services.businessmatching.BusinessMatchingService
import utils.{AuthorisedFixture, DependencyMocks, AmlsSpec}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AddBusinessTypeSummaryControllerSpec extends AmlsSpec
  with MockitoSugar
  with TradingPremisesGenerator
  with BusinessMatchingGenerator {

  sealed trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val request = addToken(authRequest)
    val mockTradingPremisesService = mock[TradingPremisesService]
    val mockUpdateServicesSummaryControllerHelper = mock[AddBusinessTypeHelper]
    val mockBusinessMatchingService = mock[BusinessMatchingService]
    val mockUpdateServiceHelper = mock[AddBusinessTypeHelper]

    val controller = new AddBusinessTypeSummaryController(
      authConnector = self.authConnector,
      dataCacheConnector = mockCacheConnector,
      statusService = mockStatusService,
      businessMatchingService = mockBusinessMatchingService,
      helper = mockUpdateServiceHelper,
      router = createRouter[AddBusinessTypeFlowModel],
      tradingPremisesService = mockTradingPremisesService
    )

    val flowModel = AddBusinessTypeFlowModel(
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

        // scalastyle:off magic.number
        val tradingPremises: Seq[TradingPremises] = Gen.listOfN(5, tradingPremisesGen).sample.get

        val modifiedTradingPremises = tradingPremises map {
          _.copy(
            whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing)))
          )
        }

        val businessMatchingModel = businessMatchingGen.sample.get.copy(
          activities = Some(BusinessActivities(Set(BillPaymentServices)))
        )

        val serviceChangeRegister: ServiceChangeRegister = ServiceChangeRegister(
          Some(Set(BillPaymentServices))
        )

        when {
          controller.helper.updateTradingPremises(eqTo(flowModel))(any(), any())
        } thenReturn OptionT.fromOption[Future](Some(modifiedTradingPremises))

        when {
          controller.helper.updateBusinessMatching(any())(any(), any())
        } thenReturn OptionT.fromOption[Future](Some(businessMatchingModel))

        when {
          controller.helper.updateServicesRegister(any())(any(), any())
        } thenReturn OptionT.some[Future, ServiceChangeRegister](serviceChangeRegister)

        when {
          controller.tradingPremisesService.updateTradingPremises(eqTo(Seq(0)), eqTo(tradingPremises), eqTo(HighValueDealing), eqTo(None), eqTo(false))
        } thenReturn modifiedTradingPremises

        when {
          controller.helper.updateHasAcceptedFlag(eqTo(flowModel))(any(), any())
        } thenReturn OptionT.fromOption[Future](Some(mockCacheMap))

        when {
          controller.helper.updateBusinessActivities(any())(any(), any())
        } thenReturn OptionT.some[Future, models.businessactivities.BusinessActivities](mock[models.businessactivities.BusinessActivities])

        when {
          controller.helper.updateSupervision(any(), any())
        } thenReturn OptionT.some[Future, Supervision](Supervision())

        when {
          controller.helper.updateResponsiblePeople(any())(any(), any())
        } thenReturn OptionT.some[Future, Seq[ResponsiblePerson]](Seq.empty)

        when {
          controller.helper.clearFlowModel()(any(), any())
        } thenReturn OptionT.some[Future, AddBusinessTypeFlowModel](AddBusinessTypeFlowModel())

        val result = controller.post()(request)

        status(result) must be(SEE_OTHER)

        controller.router.verify(AddBusinessTypeSummaryPageId, flowModel)
      }
    }
  }
}

