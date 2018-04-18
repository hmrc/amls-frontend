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
import models.businessmatching.updateservice.{ResponsiblePeopleFitAndProper, TradingPremisesActivities}
import models.businessmatching.{BillPaymentServices, BusinessActivity, HighValueDealing, TelephonePaymentService, _}
import models.flowmanagement._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Results.Redirect
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import utils.DependencyMocks

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AddTCSPSpecificRouterSpec extends PlaySpec {

  val model = AddServiceFlowModel(activity = Some(TrustAndCompanyServices))

  trait Fixture extends DependencyMocks {
    val businessMatchingService = mock[BusinessMatchingService]
    val router = new VariationAddServiceRouter(businessMatchingService)
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
  }

  "When Editing in the Add TCSP Add flow the getRoute method" must {
    //Edit mode TSCP sub-flow
    //edit fit and proper
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
