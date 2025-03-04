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

package services.flowmanagement.flowrouters

import cats.data.OptionT
import controllers.businessmatching.updateservice.add.{routes => addRoutes}
import models.businessmatching.BusinessActivity._
import models.businessmatching._
import models.flowmanagement._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.mvc.Results.Redirect
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.flowrouters.businessmatching.AddBusinessTypeRouter
import services.flowmanagement.pagerouters.addflow._
import utils.{AmlsSpec, DependencyMocks}

import scala.concurrent.Future

class AddBusinessTypeRouterSpec extends AmlsSpec {

  trait Fixture extends DependencyMocks {

    val mockBusinessMatchingService = mock[BusinessMatchingService]

    val router = new AddBusinessTypeRouter(
      businessMatchingService = mockBusinessMatchingService,
      addMoreActivitiesPageRouter = new AddMoreBusinessTypesPageRouter(mockStatusService, mockBusinessMatchingService),
      businessAppliedForPSRNumberPageRouter =
        new BusinessAppliedForPsrNumberPageRouter(mockStatusService, mockBusinessMatchingService),
      newServicesInformationPageRouter = new NeedMoreInformationPageRouter(),
      noPSRPageRouter = new NoPSRPageRouter(mockStatusService, mockBusinessMatchingService),
      selectActivitiesPageRouter = new SelectBusinessTypesPageRouter(mockStatusService, mockBusinessMatchingService),
      subServicesPageRouter = new SubSectorsPageRouter(mockStatusService, mockBusinessMatchingService),
      updateServicesSummaryPageRouter =
        new AddBusinessTypeSummaryPageRouter(mockStatusService, mockBusinessMatchingService)
    )
  }

  "getRoute" must {

    "return the 'trading premises' page (TradingPremisesController)" when {
      "given the 'BusinessActivities' model contains a single activity" in new Fixture {
        val model  = AddBusinessTypeFlowModel(activity = Some(HighValueDealing))
        val result = await(router.getRoute("internalId", SelectBusinessTypesPageId, model))

        result mustBe Redirect(addRoutes.AddBusinessTypeSummaryController.get())
      }
    }

    "return the 'trading premises' page (TradingPremisesController)" when {
      "given the 'BusinessActivities' model contains a single activity " +
        "and there is no trading premises question data in edit mode" in new Fixture {
          val model  = AddBusinessTypeFlowModel(Some(HighValueDealing))
          val result = await(router.getRoute("internalId", SelectBusinessTypesPageId, model, edit = true))

          result mustBe Redirect(addRoutes.AddBusinessTypeSummaryController.get())
        }
    }

    "return the 'Check your answers' page (UpdateServicesSummaryController)" when {
      "given the activity is not done at any trading premises " +
        "and the activity requires further information" in new Fixture {
          val model = AddBusinessTypeFlowModel(activity = Some(HighValueDealing))

          val result = await(router.getRoute("internalId", SelectBusinessTypesPageId, model))

          result mustBe Redirect(addRoutes.AddBusinessTypeSummaryController.get())
        }
    }

    "return the 'Check your answers' page (UpdateServicesSummaryController)" when {
      "given we've chosen an activity " +
        "and we're in the edit flow" in new Fixture {
          val model = AddBusinessTypeFlowModel(activity = Some(HighValueDealing))

          val result = await(router.getRoute("internalId", SelectBusinessTypesPageId, model, edit = true))

          result mustBe Redirect(addRoutes.AddBusinessTypeSummaryController.get())
        }
    }

    "return the 'Check your answers' page (UpdateServicesSummaryController)" when {
      "editing the trading premises yes/no question " +
        "the trading premises have already been selected" in new Fixture {
          val model = AddBusinessTypeFlowModel(activity = Some(HighValueDealing))

          val result = await(router.getRoute("internalId", SelectBusinessTypesPageId, model, edit = true))

          result mustBe Redirect(addRoutes.AddBusinessTypeSummaryController.get())
        }
    }

    "return the 'which trading premises' page (WhichTradingPremisesController)" when {
      "given the 'NewActivitiesAtTradingPremisesYes' model contains HVD" in new Fixture {
        val model  = AddBusinessTypeFlowModel(activity = Some(HighValueDealing))
        val result = await(router.getRoute("internalId", SelectBusinessTypesPageId, model))

        result mustBe Redirect(addRoutes.AddBusinessTypeSummaryController.get())
      }
    }

    "return the 'Check your answers' page (UpdateServicesSummaryController)" when {
      "given a set of trading premises has been chosen" in new Fixture {
        val model = AddBusinessTypeFlowModel(
          activity = Some(HighValueDealing)
        )

        val result = await(router.getRoute("internalId", SelectBusinessTypesPageId, model))

        result mustBe Redirect(addRoutes.AddBusinessTypeSummaryController.get())
      }
    }

    "return the 'Do you want add more activities' page (addMoreActivitiesController)" when {
      "we're on the summary page and the user selects continue" in new Fixture {
        val model = AddBusinessTypeFlowModel(activity = Some(HighValueDealing))

        when {
          router.businessMatchingService.getRemainingBusinessActivities(any())(any())
        } thenReturn OptionT.liftF[Future, Set[BusinessActivity]](Future.successful(Set(TelephonePaymentService)))

        val result = await(router.getRoute("internalId", AddBusinessTypeSummaryPageId, model))

        result mustBe Redirect(addRoutes.AddMoreBusinessTypesController.get())
      }
    }

    "redirect to the 'Registration Progress' page" when {
      "we're on the summary page and the user selects continue " +
        "if all possible activities are added" in new Fixture {
          when {
            router.businessMatchingService.getRemainingBusinessActivities(any())(any())
          } thenReturn OptionT.liftF[Future, Set[BusinessActivity]](Future.successful(Set.empty))

          when {
            router.businessMatchingService.getAdditionalBusinessActivities(any())(any())
          } thenReturn OptionT.liftF[Future, Set[BusinessActivity]](Future.successful(Set(BillPaymentServices)))

          val result = await(
            router
              .getRoute("internalId", AddBusinessTypeSummaryPageId, AddBusinessTypeFlowModel(Some(BillPaymentServices)))
          )

          result mustBe Redirect(addRoutes.NeedMoreInformationController.get())
        }
    }

    "redirect to the 'New Service Information' page" when {
      "we're on the summary page and the user selects continue " +
        "and if all possible activities are added" in new Fixture {
          when {
            router.businessMatchingService.getRemainingBusinessActivities(any())(any())
          } thenReturn OptionT.liftF[Future, Set[BusinessActivity]](Future.successful(Set.empty))

          when {
            router.businessMatchingService.getAdditionalBusinessActivities(any())(any())
          } thenReturn OptionT.liftF[Future, Set[BusinessActivity]](Future.successful(BusinessActivities.all))

          val result = await(
            router
              .getRoute("internalId", AddBusinessTypeSummaryPageId, AddBusinessTypeFlowModel(Some(HighValueDealing)))
          )

          result mustBe Redirect(addRoutes.NeedMoreInformationController.get())
        }
    }

    "return the 'Activities selection' page (SelectActivitiesController)" when {
      "we're on the 'Do you want at add more activities' page " +
        "and the user wants to add more activities" in new Fixture {
          val model = AddBusinessTypeFlowModel(activity = Some(HighValueDealing), addMoreActivities = Some(true))

          val result = await(router.getRoute("internalId", AddMoreBusinessTypesPageId, model))

          result mustBe Redirect(addRoutes.SelectBusinessTypeController.get())
        }
    }

    "return the 'New Service questions' page (NewServiceInformationController)" when {
      "we're on the 'Do you want at add more activities' page " +
        "and the user has added Activities that require more questions " +
        "and the use doesn't want to add more activities" in new Fixture {

          val model = AddBusinessTypeFlowModel(activity = Some(HighValueDealing), addMoreActivities = Some(false))

          when {
            router.businessMatchingService.getAdditionalBusinessActivities(any())(any())
          } thenReturn OptionT.liftF[Future, Set[BusinessActivity]](
            Future.successful(Set(HighValueDealing, BillPaymentServices))
          )

          val result = await(router.getRoute("internalId", AddMoreBusinessTypesPageId, model))

          result mustBe Redirect(addRoutes.NeedMoreInformationController.get())
        }
    }

    "return the 'New Service questions' page (NewServiceInformationController)" when {
      "we're on the 'Do you want at add more activities' page " +
        "and the user has added Activity that has no own section" +
        "and the user doesn't want to add more activities" in new Fixture {
          val model = AddBusinessTypeFlowModel(activity = Some(BillPaymentServices), addMoreActivities = Some(false))

          when {
            router.businessMatchingService.getAdditionalBusinessActivities(any())(any())
          } thenReturn OptionT.liftF[Future, Set[BusinessActivity]](
            Future.successful(Set(TelephonePaymentService, BillPaymentServices))
          )

          val result = await(router.getRoute("internalId", AddMoreBusinessTypesPageId, model))

          result mustBe Redirect(addRoutes.NeedMoreInformationController.get())
        }
    }

    "return the 'registration progress' page" when {
      "we're on the 'new service information' page" in new Fixture {
        val model = AddBusinessTypeFlowModel(activity = Some(HighValueDealing))

        val result = await(router.getRoute("internalId", NeedMoreInformationPageId, model))

        result mustBe Redirect(controllers.routes.RegistrationProgressController.get())
      }
    }
  }

}
