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

import controllers.businessmatching.updateservice.add.{routes => addRoutes}
import models.businessmatching._
import models.businessmatching.updateservice.{ResponsiblePeopleFitAndProper, TradingPremisesActivities}
import models.flowmanagement._
import play.api.mvc.Results.Redirect
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.pagerouters._
import services.flowmanagement.pagerouters.addflow._
import services.flowmanagement.pagerouters.removeflow._
import utils.{AmlsSpec, DependencyMocks}

import scala.concurrent.ExecutionContext.Implicits.global

class AddTCSPSpecificRouterSpec extends AmlsSpec {

  val model = AddServiceFlowModel(activity = Some(TrustAndCompanyServices))

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
      subServicesPageRouter = new MsbServicesPageRouter(mockStatusService, mockBusinessMatchingService),
      tradingPremisesPageRouter = new TradingPremisesPageRouter(mockStatusService, mockBusinessMatchingService),
      changeServicesPageRouter = new ChangeServicesPageRouter(mockStatusService, mockBusinessMatchingService),
      updateServicesSummaryPageRouter = new AddBusinessTypeSummaryPageRouter(mockStatusService, mockBusinessMatchingService),
      whatDoYouDoHerePageRouter = new WhatDoYouDoHerePageRouter(mockStatusService, mockBusinessMatchingService),
      whichFitAndProperPageRouter = new WhichFitAndProperPageRouter(mockStatusService, mockBusinessMatchingService),
      whichTradingPremisesPageRouter = new WhichTradingPremisesPageRouter(mockStatusService, mockBusinessMatchingService)

    )
  }

  "In the Add TCSP flow the getRoute method" must {
    //Start TSCP sub-flow

    "return the 'fit-and-proper' page (FitAndProperController)" when {
      "the user is on the 'What Type of business ....' page (SelectActivitiesPageId)" when {
        "TCSP is selected" in new Fixture {

          val result = await(router.getRoute(SelectActivitiesPageId, model))

          result mustBe Redirect(addRoutes.FitAndProperController.get(false))
        }
      }
    }

    "return the 'which-fit-and-proper' page (WhichFitAndProperController)" when {
      "the user is on the 'Fit and proper' page (FitAndProperPageId)" when {
        "TCSP is the Business Activity" when {
          "the answer is yes" in new Fixture {
            val model = AddServiceFlowModel(
              activity = Some(TrustAndCompanyServices),
              fitAndProper = Some(true))
            val result = await(router.getRoute(FitAndProperPageId, model))

            result mustBe Redirect(addRoutes.WhichFitAndProperController.get(false))
          }
        }
      }
    }

    "return the 'trading-premises' page (TradingPremisesController)" when {
      "the user is on the 'Fit and Proper' page (FitAndProperPageId)" when {
        "TCSP is the Business Activity" when {
          "the answer is no" in new Fixture {
            val model = AddServiceFlowModel(
              activity = Some(TrustAndCompanyServices),
              fitAndProper = Some(false))
            val result = await(router.getRoute(FitAndProperPageId, model))

            result mustBe Redirect(addRoutes.TradingPremisesController.get(false))
          }
        }
      }
    }

    "return the 'trading-premises' page (TradingPremisesController)" when {
      "the user is on the 'Which Fit and Proper' page (WhichFitAndProperPageId)" when {
        "TCSP is the Business Activity" in new Fixture {
          val result = await(router.getRoute(WhichFitAndProperPageId, model))

          result mustBe Redirect(addRoutes.TradingPremisesController.get(false))
        }
      }
    }

    "return the 'which-trading-premises' page (WhichTradingPremisesController)" when {
      "the user is on the 'Trading Premises' page (TradingPremisesPageId)" when {
        "TCSP is the Business Activity" when {
          "the answer is yes" in new Fixture {
            val model = AddServiceFlowModel(
              activity = Some(TrustAndCompanyServices),
              areNewActivitiesAtTradingPremises = Some(true))

            val result = await(router.getRoute(TradingPremisesPageId, model))

            result mustBe Redirect(addRoutes.WhichTradingPremisesController.get(false))
          }
        }
      }
    }

    "return the 'update_services_summary' page (UpdateServicesSummaryController)" when {
      "the user is on the 'Which Trading Premises' page (TradingPremisesPageId)" when {
        "TCSP is the Business Activity" when {
          "the answer is no" in new Fixture {
            val model = AddServiceFlowModel(
              activity = Some(TrustAndCompanyServices),
              areNewActivitiesAtTradingPremises = Some(false))

            val result = await(router.getRoute(TradingPremisesPageId, model))

            result mustBe Redirect(addRoutes.UpdateServicesSummaryController.get())
          }
        }
      }
    }

    "return the 'update_services_summary' page (UpdateServicesSummaryController)" when {
      "the user is on the 'Which Trading Premises' page (WhichTradingPremisesPageId)" when {
        "TCSP is the Business Activity" in new Fixture {
          val result = await(router.getRoute(WhichTradingPremisesPageId, model))

          result mustBe Redirect(addRoutes.UpdateServicesSummaryController.get())
        }
      }
    }
  }


  "When Editing in the TCSP Add flow, the getRoute method" must {

    //edit fit and proper true
    "return the 'which-fit-and-proper' page (WhichFitAndProperController)" when {
      "editing the 'Fit and Proper' page (FitAndProperPageId)" when {
        "and the answer is yes" in new Fixture {
          val model = AddServiceFlowModel(
            activity = Some(TrustAndCompanyServices),
            fitAndProper = Some(true))
          val result = await(router.getRoute(FitAndProperPageId, model, edit = true))

          result mustBe Redirect(addRoutes.WhichFitAndProperController.get(true))
        }
      }
    }

    //edit fit and proper false
    "return the 'Check your answers' page (UpdateServicesSummaryController)" when {
      "editing the 'Fit and Proper' page (FitAndProperPageId)" when {
        " and the answer is no " in new Fixture {
          val model = AddServiceFlowModel(
            activity = Some(TrustAndCompanyServices),
            fitAndProper = Some(false))
          val result = await(router.getRoute(FitAndProperPageId, model, edit = true))

          result mustBe Redirect(addRoutes.UpdateServicesSummaryController.get())
        }
      }
    }

    //edit which fit and proper
    "return the 'Check your answers' page (UpdateServicesSummaryController)" when {
      "editing the 'Which Fit and Proper' page (WhichFitAndProperPageId)" when {
        "responsible people have been selected" in new Fixture {
          val model = AddServiceFlowModel(
            activity = Some(TrustAndCompanyServices),
            fitAndProper = Some(true),
            responsiblePeople = Some(ResponsiblePeopleFitAndProper(Set(0, 1, 2, 3))))
          val result = await(router.getRoute(WhichFitAndProperPageId, model, edit = true))

          result mustBe Redirect(addRoutes.UpdateServicesSummaryController.get())
        }
      }
    }

    //edit Trading Premises true
    "return the 'which-trading-premises' page (WhichTradingPremisesController)" when {
      "editing the 'Trading Premises' page (TradingPremisesPageId)" when {
        "and the answer is yes" in new Fixture {
          val model = AddServiceFlowModel(
            activity = Some(TrustAndCompanyServices),
            areNewActivitiesAtTradingPremises = Some(true))
          val result = await(router.getRoute(TradingPremisesPageId, model, edit = true))

          result mustBe Redirect(addRoutes.WhichTradingPremisesController.get(true))
        }
      }
    }

    //edit Trading Premises false
    "return the 'Check your answers' page (UpdateServicesSummaryController)" when {
      "editing the 'Trading Premises' page (TradingPremisesPageId)" when {
        " and the answer is no " in new Fixture {
          val model = AddServiceFlowModel(
            activity = Some(TrustAndCompanyServices),
            areNewActivitiesAtTradingPremises = Some(false))
          val result = await(router.getRoute(TradingPremisesPageId, model, edit = true))

          result mustBe Redirect(addRoutes.UpdateServicesSummaryController.get())
        }
      }
    }

    //edit which Trading Premises
    "return the 'Check your answers' page (UpdateServicesSummaryController)" when {
      "editing the 'Which Trading Premises' page (WhichTradingPremisesPageId)" when {
        "trading premises have been selected" in new Fixture {
          val model = AddServiceFlowModel(
            activity = Some(TrustAndCompanyServices),
            areNewActivitiesAtTradingPremises = Some(true),
            tradingPremisesActivities = Some(TradingPremisesActivities(Set(0, 1, 2, 3))))
          val result = await(router.getRoute(WhichTradingPremisesPageId, model, edit = true))

          result mustBe Redirect(addRoutes.UpdateServicesSummaryController.get())
        }
      }
    }
  }
}
