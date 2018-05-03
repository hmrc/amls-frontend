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
import models.businessmatching._
import models.businessmatching.updateservice.{ResponsiblePeopleFitAndProper, TradingPremisesActivities}
import models.flowmanagement._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Results.Redirect
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.pagerouters._
import utils.{DependencyMocks, GenericTestHelper}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DavesVariationAddServiceRouterSpec extends GenericTestHelper {

  trait Fixture extends DependencyMocks {

    val mockBusinessMatchingService = mock[BusinessMatchingService]

    val router = new DavesVariationAddServiceRouter(
      statusService = mockStatusService,
      businessMatchingService = mockBusinessMatchingService,
      addMoreActivitiesPageRouter = new AddMoreActivitiesPageRouter(mockStatusService, mockBusinessMatchingService),
      fitAndProperPageRoutes = new FitAndProperPageRouter(mockStatusService, mockBusinessMatchingService),
      newServicesInformationPageRouter = new NewServicesInformationPageRouter(mockStatusService, mockBusinessMatchingService),
      selectActivitiesPageRouter = new SelectActivitiesPageRouter(mockStatusService, mockBusinessMatchingService),
      tradingPremisesPageRouter = new TradingPremisesPageRouter(mockStatusService, mockBusinessMatchingService),
      updateServicesSummaryPageRoutes =new UpdateServicesSummaryPageRouter(mockStatusService, mockBusinessMatchingService),
      whichFitAndProperPageRoutes = new WhichFitAndProperPageRouter(mockStatusService, mockBusinessMatchingService),
      whichTradingPremisesPageRouter = new WhichTradingPremisesPageRouter(mockStatusService, mockBusinessMatchingService)

    )
  }

  "getRoute" must {

    "return the 'trading premises' page (TradingPremisesController)" when {
      "given the 'BusinessActivities' model contains a single activity" in new Fixture {
        val model = AddServiceFlowModel(
          activity = Some(HighValueDealing))
        val result = await(router.getRoute(SelectActivitiesPageId, model))

        result mustBe Redirect(addRoutes.TradingPremisesController.get())
      }
    }

    "return the 'trading premises' page (TradingPremisesController)" when {
      "given the 'BusinessActivities' model contains a single activity " +
        "and there is no trading premises question data in edit mode" in new Fixture {
        val model = AddServiceFlowModel(Some(HighValueDealing))
        val result = await(router.getRoute(SelectActivitiesPageId, model, edit = true))

        result mustBe Redirect(addRoutes.TradingPremisesController.get())
      }
    }

    "return the 'Check your answers' page (UpdateServicesSummaryController)" when {
      "given the activity is not done at any trading premises " +
        "and the activity requires further information" in new Fixture {
        val model = AddServiceFlowModel(
          activity = Some(HighValueDealing),
          areNewActivitiesAtTradingPremises = Some(false))

        val result = await(router.getRoute(TradingPremisesPageId, model))

        result mustBe Redirect(addRoutes.UpdateServicesSummaryController.get())
      }
    }

    "return the 'Check your answers' page (UpdateServicesSummaryController)" when {
      "given we've chosen an activity " +
        "and we're in the edit flow" in new Fixture {
        val model = AddServiceFlowModel(
          activity = Some(HighValueDealing),
          areNewActivitiesAtTradingPremises = Some(false))

        val result = await(router.getRoute(SelectActivitiesPageId, model, edit = true))

        result mustBe Redirect(addRoutes.UpdateServicesSummaryController.get())
      }
    }

    "return the 'Check your answers' page (UpdateServicesSummaryController)" when {
      "editing the trading premises yes/no question " +
        "the trading premises have already been selected" in new Fixture {
        val model = AddServiceFlowModel(
          activity = Some(HighValueDealing),
          areNewActivitiesAtTradingPremises = Some(true),
          tradingPremisesActivities = Some(TradingPremisesActivities(Set(0, 1))))

        val result = await(router.getRoute(TradingPremisesPageId, model, edit = true))

        result mustBe Redirect(addRoutes.UpdateServicesSummaryController.get())
      }
    }

    "return the 'which trading premises' page (WhichTradingPremisesController)" when {
      "given the 'NewActivitiesAtTradingPremisesYes' model contains HVD" in new Fixture {
        val model = AddServiceFlowModel(
          activity = Some(HighValueDealing),
          areNewActivitiesAtTradingPremises = Some(true))
        val result = await(router.getRoute(TradingPremisesPageId, model))

        result mustBe Redirect(addRoutes.WhichTradingPremisesController.get())
      }
    }

    "return the 'Check your answers' page (UpdateServicesSummaryController)" when {
      "given a set of trading premises has been chosen" in new Fixture {
        val model = AddServiceFlowModel(
          activity = Some(HighValueDealing),
          areNewActivitiesAtTradingPremises = Some(true),
          tradingPremisesActivities = Some(TradingPremisesActivities(Set(0, 1, 2)))
        )

        val result = await(router.getRoute(WhichTradingPremisesPageId, model))

        result mustBe Redirect(addRoutes.UpdateServicesSummaryController.get())
      }
    }

    "return the 'Do you want add more activities' page (addMoreActivitiesController)" when {
      "we're on the summary page and the user selects continue" in new Fixture {
        val model = AddServiceFlowModel(
          activity = Some(HighValueDealing),
          areNewActivitiesAtTradingPremises = Some(true))

        when {
          router.businessMatchingService.getRemainingBusinessActivities(any(), any(), any())
        } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(TelephonePaymentService))

        val result = await(router.getRoute(UpdateServiceSummaryPageId, model))

        result mustBe Redirect(addRoutes.AddMoreActivitiesController.get())
      }
    }

    "redirect to the 'Registration Progress' page" when {
      "we're on the summary page and the user selects continue " +
        "if all possible activities are added" +
        " and the new activity does not require more information" in new Fixture {
        when {
          router.businessMatchingService.getRemainingBusinessActivities(any(), any(), any())
        } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set.empty)

        val result = await(router.getRoute(UpdateServiceSummaryPageId, AddServiceFlowModel(Some(BillPaymentServices))))

        result mustBe Redirect(controllers.routes.RegistrationProgressController.get())
      }
    }

    "redirect to the 'New Service Information' page" when {
      "we're on the summary page and the user selects continue " +
        "and if all possible activities are added" +
        " and the new one requires more information" in new Fixture {

        when {
          router.businessMatchingService.getRemainingBusinessActivities(any(), any(), any())
        } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set.empty)

        val result = await(router.getRoute(UpdateServiceSummaryPageId, AddServiceFlowModel(Some(HighValueDealing))))

        result mustBe Redirect(addRoutes.NewServiceInformationController.get())
      }
    }

    "return the 'Activities selection' page (SelectActivitiesController)" when {
      "we're on the 'Do you want at add more activities' page " +
        "and the use wants to add more activities" in new Fixture {
        val model = AddServiceFlowModel(
          activity = Some(HighValueDealing),
          areNewActivitiesAtTradingPremises = Some(true),
          addMoreActivities = Some(true))

        val result = await(router.getRoute(AddMoreAcivitiesPageId, model))

        result mustBe Redirect(addRoutes.SelectActivitiesController.get())
      }
    }

    "return the 'New Service questions' page (NewServiceInformationController)" when {
      "we're on the 'Do you want at add more activities' page " +
        "and the user has added Activities that require more questions " +
        "and the use doesn't want to add more activities" in new Fixture {

        val model = AddServiceFlowModel(
          activity = Some(HighValueDealing),
          areNewActivitiesAtTradingPremises = Some(true),
          addMoreActivities = Some(false))

        when {
          router.businessMatchingService.getAdditionalBusinessActivities(any(), any(), any())
        } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(HighValueDealing, BillPaymentServices))

        val result = await(router.getRoute(AddMoreAcivitiesPageId, model))

        result mustBe Redirect(addRoutes.NewServiceInformationController.get())
      }
    }

    "return the 'Registration progress' page (RegistrationProgressController)" when {
      "we're on the 'Do you want at add more activities' page " +
        "and the user has NOT added Activities that require more questions" +
        "and the use doesn't want to add more activities" in new Fixture {
        val model = AddServiceFlowModel(
          activity = Some(BillPaymentServices),
          addMoreActivities = Some(false))

        when {
          router.businessMatchingService.getAdditionalBusinessActivities(any(), any(), any())
        } thenReturn OptionT.some[Future, Set[BusinessActivity]](Set(TelephonePaymentService, BillPaymentServices))

        val result = await(router.getRoute(AddMoreAcivitiesPageId, model))

        result mustBe Redirect(controllers.routes.RegistrationProgressController.get())
      }
    }

    "return the 'registration progress' page" when {
      "we're on the 'new service information' page" in new Fixture {
        val model = AddServiceFlowModel(
          activity = Some(HighValueDealing),
          areNewActivitiesAtTradingPremises = Some(true))

        val result = await(router.getRoute(NewServiceInformationPageId, model))

        result mustBe Redirect(controllers.routes.RegistrationProgressController.get())
      }
    }
  }
    "when in the TCSP flow" must {
      //Start TSCP sub-flow
      "return the 'fitAndProper' page (FitAndProperController)" when {
        "the user is on the 'What Type of business ....' page (SelectActivitiesPageId)" when {
          "TCSP is selected" in new Fixture {
            val model = AddServiceFlowModel(
              activity = Some(TrustAndCompanyServices))

            val result = await(router.getRoute(SelectActivitiesPageId, model))

            result mustBe Redirect(addRoutes.FitAndProperController.get())
          }
        }
      }

      "return the 'WhichfitAndProper' page (WhichFitAndProperController)" when {
        "the user is on the 'Fit and proper' page (FitAndProperPageId)" when {
          "TCSP is the Business Activity" when {
            "the answer is yes" in new Fixture {
              val model = AddServiceFlowModel(
                activity = Some(TrustAndCompanyServices),
                fitAndProper = Some(true))
              val result = await(router.getRoute(FitAndProperPageId, model))

              result mustBe Redirect(addRoutes.WhichFitAndProperController.get())
            }
          }
        }
      }

      "return the 'tradingPremises' page (TradingPremisesController)" when {
        "the user is on the 'Fit and Proper' page (FitAndProperPageId)" when {
          "TCSP is the Business Activity" when {
            "the answer is no" in new Fixture {
              val model = AddServiceFlowModel(
                activity = Some(TrustAndCompanyServices),
                fitAndProper = Some(false))
              val result = await(router.getRoute(FitAndProperPageId, model))

              result mustBe Redirect(addRoutes.TradingPremisesController.get())
            }
          }
        }
      }

      "return the 'tradingPremises' page (TradingPremisesController)" when {
        "the user is on the Which Fit and Proper page (WhichFitAndProperPageId)" when {
          "TCSP is the Business Activity" in new Fixture {
            val model = AddServiceFlowModel(
              activity = Some(TrustAndCompanyServices))
            val result = await(router.getRoute(WhichFitAndProperPageId, model))

            result mustBe Redirect(addRoutes.TradingPremisesController.get())
          }
        }
      }

      "edit mode" must {
        //Edit mode TSCP sub-flow
        //edit fit and proper
        "return the 'which fit and proper' page (WhichFitAndProperController)" when {
          "editing the 'Fit and Proper' page (FitAndProperPageId)" when {
            "and the answer is yes" in new Fixture {
              val model = AddServiceFlowModel(
                activity = Some(TrustAndCompanyServices),
                fitAndProper = Some(true))
              val result = await(router.getRoute(FitAndProperPageId, model, edit = true))

              result mustBe Redirect(addRoutes.WhichFitAndProperController.get())
            }
          }
        }

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
      }
    }


}
