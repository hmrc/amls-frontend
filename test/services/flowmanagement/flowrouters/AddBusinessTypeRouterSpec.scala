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

package services.flowmanagement.flowrouters

import cats.data.OptionT
import cats.implicits._
import controllers.businessmatching.updateservice.add.{routes => addRoutes}
import models.businessmatching.updateservice.TradingPremisesActivities
import models.businessmatching.{BillPaymentServices, BusinessActivity, HighValueDealing, TelephonePaymentService, _}
import models.flowmanagement._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.mvc.Results.Redirect
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.pagerouters.addflow._
import utils.{AmlsSpec, DependencyMocks}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AddBusinessTypeRouterSpec extends AmlsSpec {

  trait Fixture extends DependencyMocks {

    val mockBusinessMatchingService = mock[BusinessMatchingService]

    val router = new AddBusinessTypeRouter(
      businessMatchingService = mockBusinessMatchingService,
      addMoreActivitiesPageRouter = new AddMoreBusinessTypesPageRouter(mockStatusService, mockBusinessMatchingService),
      businessAppliedForPSRNumberPageRouter = new BusinessAppliedForPsrNumberPageRouter(mockStatusService, mockBusinessMatchingService),
      fitAndProperPageRouter = new FitAndProperPageRouter(mockStatusService, mockBusinessMatchingService),
      newServicesInformationPageRouter = new NeedMoreInformationPageRouter(mockStatusService, mockBusinessMatchingService),
      noPSRPageRouter = new NoPSRPageRouter(mockStatusService, mockBusinessMatchingService),
      selectActivitiesPageRouter = new SelectBusinessTypesPageRouter(mockStatusService, mockBusinessMatchingService),
      subServicesPageRouter = new SubSectorsPageRouter(mockStatusService, mockBusinessMatchingService),
      tradingPremisesPageRouter = new TradingPremisesPageRouter(mockStatusService, mockBusinessMatchingService),
      updateServicesSummaryPageRouter = new AddBusinessTypeSummaryPageRouter(mockStatusService, mockBusinessMatchingService),
      whatDoYouDoHerePageRouter = new WhatDoYouDoHerePageRouter(mockStatusService, mockBusinessMatchingService),
      whichFitAndProperPageRouter = new WhichFitAndProperPageRouter(mockStatusService, mockBusinessMatchingService),
      whichTradingPremisesPageRouter = new WhichTradingPremisesPageRouter(mockStatusService, mockBusinessMatchingService)

    )
  }

  "getRoute" must {

    "return the 'trading premises' page (TradingPremisesController)" when {
      "given the 'BusinessActivities' model contains a single activity" in new Fixture {
        val model = AddBusinessTypeFlowModel(
          activity = Some(HighValueDealing))
        val result = await(router.getRoute(SelectBusinessTypesPageId, model))

        result mustBe Redirect(addRoutes.TradingPremisesController.get())
      }
    }

    "return the 'trading premises' page (TradingPremisesController)" when {
      "given the 'BusinessActivities' model contains a single activity " +
        "and there is no trading premises question data in edit mode" in new Fixture {
        val model = AddBusinessTypeFlowModel(Some(HighValueDealing))
        val result = await(router.getRoute(SelectBusinessTypesPageId, model, edit = true))

        result mustBe Redirect(addRoutes.TradingPremisesController.get(true))
      }
    }

    "return the 'Check your answers' page (UpdateServicesSummaryController)" when {
      "given the activity is not done at any trading premises " +
        "and the activity requires further information" in new Fixture {
        val model = AddBusinessTypeFlowModel(
          activity = Some(HighValueDealing),
          areNewActivitiesAtTradingPremises = Some(false))

        val result = await(router.getRoute(TradingPremisesPageId, model))

        result mustBe Redirect(addRoutes.AddBusinessTypeSummaryController.get())
      }
    }

    "return the 'Check your answers' page (UpdateServicesSummaryController)" when {
      "given we've chosen an activity " +
        "and we're in the edit flow" in new Fixture {
        val model = AddBusinessTypeFlowModel(
          activity = Some(HighValueDealing),
          areNewActivitiesAtTradingPremises = Some(false))

        val result = await(router.getRoute(SelectBusinessTypesPageId, model, edit = true))

        result mustBe Redirect(addRoutes.AddBusinessTypeSummaryController.get())
      }
    }

    "return the 'Check your answers' page (UpdateServicesSummaryController)" when {
      "editing the trading premises yes/no question " +
        "the trading premises have already been selected" in new Fixture {
        val model = AddBusinessTypeFlowModel(
          activity = Some(HighValueDealing),
          areNewActivitiesAtTradingPremises = Some(true),
          tradingPremisesActivities = Some(TradingPremisesActivities(Set(0, 1))))

        val result = await(router.getRoute(TradingPremisesPageId, model, edit = true))

        result mustBe Redirect(addRoutes.AddBusinessTypeSummaryController.get())
      }
    }

    "return the 'which trading premises' page (WhichTradingPremisesController)" when {
      "given the 'NewActivitiesAtTradingPremisesYes' model contains HVD" in new Fixture {
        val model = AddBusinessTypeFlowModel(
          activity = Some(HighValueDealing),
          areNewActivitiesAtTradingPremises = Some(true))
        val result = await(router.getRoute(TradingPremisesPageId, model))

        result mustBe Redirect(addRoutes.WhichTradingPremisesController.get())
      }
    }

    "return the 'Check your answers' page (UpdateServicesSummaryController)" when {
      "given a set of trading premises has been chosen" in new Fixture {
        val model = AddBusinessTypeFlowModel(
          activity = Some(HighValueDealing),
          areNewActivitiesAtTradingPremises = Some(true),
          tradingPremisesActivities = Some(TradingPremisesActivities(Set(0, 1, 2)))
        )

        val result = await(router.getRoute(WhichTradingPremisesPageId, model))

        result mustBe Redirect(addRoutes.AddBusinessTypeSummaryController.get())
      }
    }

    "return the 'Do you want add more activities' page (addMoreActivitiesController)" when {
      "we're on the summary page and the user selects continue" in new Fixture {
        val model = AddBusinessTypeFlowModel(
          activity = Some(HighValueDealing),
          areNewActivitiesAtTradingPremises = Some(true))

        when {
          router.businessMatchingService.getRemainingBusinessActivities(any(), any(), any())
        } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(TelephonePaymentService))

        val result = await(router.getRoute(AddBusinessTypeSummaryPageId, model))

        result mustBe Redirect(addRoutes.AddMoreBusinessTypesController.get())
      }
    }

    "redirect to the 'Registration Progress' page" when {
      "we're on the summary page and the user selects continue " +
        "if all possible activities are added" +
        " and the new activity does not require more information" in new Fixture {
        when {
          router.businessMatchingService.getRemainingBusinessActivities(any(), any(), any())
        } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set.empty)

        when {
          router.businessMatchingService.getAdditionalBusinessActivities(any(), any(), any())
        } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(BillPaymentServices))

        val result = await(router.getRoute(AddBusinessTypeSummaryPageId, AddBusinessTypeFlowModel(Some(BillPaymentServices))))

        result mustBe Redirect(controllers.routes.RegistrationProgressController.get())
      }
    }

    "redirect to the 'New Service Information' page" when {
      "we're on the summary page and the user selects continue " +
        "and if all possible activities are added" in new Fixture {
        when {
          router.businessMatchingService.getRemainingBusinessActivities(any(), any(), any())
        } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set.empty)

        when {
          router.businessMatchingService.getAdditionalBusinessActivities(any(), any(), any())
        } thenReturn OptionT.some[Future, Set[BusinessActivity]](BusinessActivities.all)

        val result = await(router.getRoute(AddBusinessTypeSummaryPageId, AddBusinessTypeFlowModel(Some(HighValueDealing))))

        result mustBe Redirect(addRoutes.NeedMoreInformationController.get())
      }
    }

    "return the 'Activities selection' page (SelectActivitiesController)" when {
      "we're on the 'Do you want at add more activities' page " +
        "and the user wants to add more activities" in new Fixture {
        val model = AddBusinessTypeFlowModel(
          activity = Some(HighValueDealing),
          areNewActivitiesAtTradingPremises = Some(true),
          addMoreActivities = Some(true))

        val result = await(router.getRoute(AddMoreBusinessTypesPageId, model))

        result mustBe Redirect(addRoutes.SelectBusinessTypeController.get())
      }
    }

    "return the 'New Service questions' page (NewServiceInformationController)" when {
      "we're on the 'Do you want at add more activities' page " +
        "and the user has added Activities that require more questions " +
        "and the use doesn't want to add more activities" in new Fixture {

        val model = AddBusinessTypeFlowModel(
          activity = Some(HighValueDealing),
          areNewActivitiesAtTradingPremises = Some(true),
          addMoreActivities = Some(false))

        when {
          router.businessMatchingService.getAdditionalBusinessActivities(any(), any(), any())
        } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(HighValueDealing, BillPaymentServices))

        val result = await(router.getRoute(AddMoreBusinessTypesPageId, model))

        result mustBe Redirect(addRoutes.NeedMoreInformationController.get())
      }
    }

    "return the 'Registration progress' page (RegistrationProgressController)" when {
      "we're on the 'Do you want at add more activities' page " +
        "and the user has NOT added Activities that require more questions" +
        "and the use doesn't want to add more activities" in new Fixture {
        val model = AddBusinessTypeFlowModel(
          activity = Some(BillPaymentServices),
          addMoreActivities = Some(false))

        when {
          router.businessMatchingService.getAdditionalBusinessActivities(any(), any(), any())
        } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(TelephonePaymentService, BillPaymentServices))

        val result = await(router.getRoute(AddMoreBusinessTypesPageId, model))

        result mustBe Redirect(controllers.routes.RegistrationProgressController.get())
      }
    }

    "return the 'registration progress' page" when {
      "we're on the 'new service information' page" in new Fixture {
        val model = AddBusinessTypeFlowModel(
          activity = Some(HighValueDealing),
          areNewActivitiesAtTradingPremises = Some(true))

        val result = await(router.getRoute(NeedMoreInformationPageId, model))

        result mustBe Redirect(controllers.routes.RegistrationProgressController.get())
      }
    }
  }

}
