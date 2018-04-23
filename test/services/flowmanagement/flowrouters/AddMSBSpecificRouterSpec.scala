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
import models.flowmanagement._
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Results.Redirect
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import utils.DependencyMocks

import scala.concurrent.ExecutionContext.Implicits.global

class AddMSBSpecificRouterSpec extends PlaySpec {

  val model = AddServiceFlowModel(activity = Some(MoneyServiceBusiness))

  trait Fixture extends DependencyMocks {
    val businessMatchingService = mock[BusinessMatchingService]
    val router = new VariationAddServiceRouter(businessMatchingService)
  }

  "In the Add MSB flow the getRoute method" must {
    //Start MSB sub-flow

    "return the 'msb_subservice' page (SubServiceController)" when {
      "the user is on the 'What Type of business ....' page (SelectActivitiesPageId)" when {
        "MSB is selected" in new Fixture {

          val result = await(router.getRoute(SelectActivitiesPageId, model))

          result mustBe Redirect(addRoutes.SubServicesController.get(false))
        }
      }
    }
  

    "return the 'business_applied_for_psr_number' page (BusinessAppliedForPSRNumberController)" when {
      "the user is on the 'msb_subservice' page (SubServicePageId)" when {
        "MSB is the Business Activity"  in new Fixture {
            val model = AddServiceFlowModel(
              activity = Some(MoneyServiceBusiness))
            val result = await(router.getRoute(SubServicesPageId, model))

            result mustBe Redirect(addRoutes.BusinessAppliedForPSRNumberController.get(false))
          }
        }
    }

    "return the 'no-psr' page (NoPsrController)" when {
      "the user is on the 'business_applied_for_psr_number' page (BusinessAppliedForPSRNumberPageId)" when {
        "the answer is no and MSB is the Business Activity" in new Fixture {
          val model = AddServiceFlowModel(
            activity = Some(MoneyServiceBusiness),
            businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberNo))
          // Need to add PSR model property
          val result = await(router.getRoute(BusinessAppliedForPSRNumberPageId, model))

          result mustBe Redirect(addRoutes.NoPsrController.get())
        }
      }
    }

    "return the 'FitAndProper' page (FitAndProperController)" when {
      "the user is on the 'business_applied_for_psr_number' page (BusinessAppliedForPSRNumberPageId)" when {
        "the answer is yes and MSB is the Business Activity" in new Fixture {
          val model = AddServiceFlowModel(
            activity = Some(MoneyServiceBusiness),
            businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("aaaaa")))
          // Need to add PSR model property
          val result = await(router.getRoute(BusinessAppliedForPSRNumberPageId, model))

          result mustBe Redirect(addRoutes.FitAndProperController.get(false))
        }
      }
    }

    "return the 'FitAndProper' page (FitAndProperController)" when {
      "the user is on the 'no_psr' page (NoPSRPageId)" when {
        "MSB is the Business Activity" in new Fixture {
          val model = AddServiceFlowModel(
            activity = Some(TrustAndCompanyServices))
          val result = await(router.getRoute(NoPSRPageId, model))

          result mustBe Redirect(addRoutes.FitAndProperController.get(false))
        }
      }
    }
//
//  "When Editing in the Add TCSP Add flow the getRoute method" must {
//    //Edit mode TSCP sub-flow
//    //edit fit and proper
//    "return the 'which-fit-and-proper' page (WhichFitAndProperController)" when {
//      "editing the 'Fit and Proper' page (FitAndProperPageId)" when {
//        "and the answer is yes" in new Fixture {
//          val model = AddServiceFlowModel(
//            activity = Some(TrustAndCompanyServices),
//            fitAndProper = Some(true))
//          val result = await(router.getRoute(FitAndProperPageId, model, edit = true))
//
//          result mustBe Redirect(addRoutes.WhichFitAndProperController.get(true))
//        }
//      }
//    }
//
//    "return the 'Check your answers' page (UpdateServicesSummaryController)" when {
//      "editing the 'Fit and Proper' page (FitAndProperPageId)" when {
//        " and the answer is no " in new Fixture {
//          val model = AddServiceFlowModel(
//            activity = Some(TrustAndCompanyServices),
//            fitAndProper = Some(false))
//          val result = await(router.getRoute(FitAndProperPageId, model, edit = true))
//
//          result mustBe Redirect(addRoutes.UpdateServicesSummaryController.get())
//        }
//      }
//    }
//    //edit which fit and proper
//    "return the 'Check your answers' page (UpdateServicesSummaryController)" when {
//      "editing the 'Which Fit and Proper' page (WhichFitAndProperPageId)" when {
//        "responsible people have been selected" in new Fixture {
//          val model = AddServiceFlowModel(
//            activity = Some(TrustAndCompanyServices),
//            fitAndProper = Some(true),
//            responsiblePeople = Some(ResponsiblePeopleFitAndProper(Set(0, 1, 2, 3))))
//          val result = await(router.getRoute(WhichFitAndProperPageId, model, edit = true))
//
//          result mustBe Redirect(addRoutes.UpdateServicesSummaryController.get())
//        }
//      }
//    }
  }
}
