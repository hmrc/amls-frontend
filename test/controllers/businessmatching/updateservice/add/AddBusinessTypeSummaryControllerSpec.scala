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

package controllers.businessmatching.updateservice.add

import cats.data.OptionT
import controllers.actions.SuccessfulAuthAction
import controllers.businessmatching.updateservice.AddBusinessTypeHelper
import generators.ResponsiblePersonGenerator
import generators.businessmatching.BusinessMatchingGenerator
import generators.tradingpremises.TradingPremisesGenerator
import models.businessmatching._
import models.businessmatching.BusinessActivity.{BillPaymentServices, HighValueDealing, TrustAndCompanyServices}
import models.businessmatching.updateservice.ServiceChangeRegister
import models.flowmanagement.{AddBusinessTypeFlowModel, AddBusinessTypeSummaryPageId}
import models.status.SubmissionDecisionApproved
import models.supervision.Supervision
import models.tradingpremises.{TradingPremises, WhatDoesYourBusinessDo}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.TradingPremisesService
import services.businessmatching.BusinessMatchingService
import utils.{AmlsSpec, DependencyMocks}
import views.html.businessmatching.updateservice.add.UpdateServicesSummaryView

import scala.concurrent.Future

class AddBusinessTypeSummaryControllerSpec
    extends AmlsSpec
    with MockitoSugar
    with TradingPremisesGenerator
    with BusinessMatchingGenerator
    with ResponsiblePersonGenerator {

  sealed trait Fixture extends DependencyMocks {
    self =>

    val request                                   = addToken(authRequest)
    val mockTradingPremisesService                = mock[TradingPremisesService]
    val mockUpdateServicesSummaryControllerHelper = mock[AddBusinessTypeHelper]
    val mockBusinessMatchingService               = mock[BusinessMatchingService]
    val mockUpdateServiceHelper                   = mock[AddBusinessTypeHelper]

    lazy val view  = app.injector.instanceOf[UpdateServicesSummaryView]
    val controller = new AddBusinessTypeSummaryController(
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      dataCacheConnector = mockCacheConnector,
      statusService = mockStatusService,
      businessMatchingService = mockBusinessMatchingService,
      helper = mockUpdateServiceHelper,
      router = createRouter[AddBusinessTypeFlowModel],
      tradingPremisesService = mockTradingPremisesService,
      cc = mockMcc,
      view = view
    )

    val flowModel = AddBusinessTypeFlowModel(
      activity = Some(TrustAndCompanyServices),
      addMoreActivities = None,
      hasChanged = true,
      hasAccepted = false,
      businessAppliedForPSRNumber = None,
      subSectors = None
    )

    mockCacheFetch[AddBusinessTypeFlowModel](Some(flowModel))

    mockApplicationStatus(SubmissionDecisionApproved)
  }

  "UpdateServicesSummaryController" when {

    "get is called" must {
      "return OK with update_service_summary view" in new Fixture {
        val result = controller.get()(request)

        status(result)          must be(OK)
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
            controller.helper.updateBusinessMatching(any(), any())(any())
          } thenReturn OptionT.fromOption[Future](Some(businessMatchingModel))

          when {
            controller.helper.updateServicesRegister(any(), any())(any())
          } thenReturn OptionT.liftF(Future.successful(serviceChangeRegister))

          when {
            controller.tradingPremisesService.updateTradingPremises(
              eqTo(Seq(0)),
              eqTo(tradingPremises),
              eqTo(HighValueDealing),
              eqTo(None),
              eqTo(false)
            )
          } thenReturn modifiedTradingPremises

          when {
            controller.helper.updateHasAcceptedFlag(any(), eqTo(flowModel))(any())
          } thenReturn OptionT.fromOption[Future](Some(mockCacheMap))

          when {
            controller.helper.updateBusinessActivities(any(), any())
          } thenReturn OptionT.liftF(Future.successful(mock[models.businessactivities.BusinessActivities]))

          when {
            controller.helper.updateSupervision(any())(any())
          } thenReturn OptionT.liftF(Future.successful(Supervision()))

          when {
            controller.helper.clearFlowModel(any())
          } thenReturn OptionT.liftF(Future.successful(AddBusinessTypeFlowModel()))

          val result = controller.post()(request)

          status(result) must be(SEE_OTHER)

          controller.router.verify("internalId", AddBusinessTypeSummaryPageId, flowModel)
        }
    }
  }
}
