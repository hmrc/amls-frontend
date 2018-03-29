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

package services.flowmanagement.routing

import models.businessmatching.updateservice.{NewActivitiesAtTradingPremisesNo, NewActivitiesAtTradingPremisesYes, TradingPremisesActivities}
import models.businessmatching.{BillPaymentServices, BusinessActivities, HighValueDealing}
import models.flowmanagement._
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Results.Redirect
import services.flowmanagement._
import services.flowmanagement.routings.VariationAddServiceRouter._

class VariationAddServiceRouterSpec extends PlaySpec {

  trait Fixture {
    val routingFile = implicitly[Router[AddServiceFlowModel]]
  }

  "getRoute" must {

    "return the 'trading premises' page" when {
      "given the 'BusinessActivities' model contains a single activity" in new Fixture {
        val model = AddServiceFlowModel(Some(BusinessActivities(Set(HighValueDealing))))
        val result = routingFile.getRoute(BusinessActivitiesSelectionPageId, model)

        result mustBe Redirect(controllers.businessmatching.updateservice.routes.TradingPremisesController.get(0))
      }
    }

    "return the 'Check your answers' page" when {
      "given the activity is not done at any trading premises" in new Fixture {
        val model = AddServiceFlowModel(Some(BusinessActivities(Set(HighValueDealing))), Some(NewActivitiesAtTradingPremisesNo))
        val result = routingFile.getRoute(TradingPremisesDecisionPageId, model)

        result mustBe Redirect(controllers.businessmatching.updateservice.routes.UpdateServicesSummaryController.get())
      }
    }

    "return the 'which trading premises' page" when {
      "given the 'NewActivitiesAtTradingPremisesYes' model contains HVD" in new Fixture {
        val model = AddServiceFlowModel(Some(BusinessActivities(Set(HighValueDealing))), Some(NewActivitiesAtTradingPremisesYes(HighValueDealing)))
        val result = routingFile.getRoute(TradingPremisesDecisionPageId, model)

        result mustBe Redirect(controllers.businessmatching.updateservice.routes.WhichTradingPremisesController.get(0))
      }
    }

    "return the 'Check your answers' page" when {
      "given a set of trading premises has been chosen" in new Fixture {
        val model = AddServiceFlowModel(Some(BusinessActivities(Set(HighValueDealing))),
          Some(NewActivitiesAtTradingPremisesYes(HighValueDealing)),
          Some(TradingPremisesActivities(Set(0,1,2)))
        )

        val result = routingFile.getRoute(TradingPremisesSelectionPageId, model)

        result mustBe Redirect(controllers.businessmatching.updateservice.routes.UpdateServicesSummaryController.get())
      }
    }

    "return the 'new service information' page" when {
      "we're on the summary page and a business activity has been chosen which requires more questions" in new Fixture {
        val model = AddServiceFlowModel(
          Some(BusinessActivities(Set(HighValueDealing))),
          Some(NewActivitiesAtTradingPremisesYes(HighValueDealing)))

        val result = routingFile.getRoute(AddServiceSummaryPageId, model)

        result mustBe Redirect(controllers.businessmatching.updateservice.routes.NewServiceInformationController.get())
      }
    }

    "return the 'registration progress' page" when {
      "we're on the summary page and 'Bill Payments' has been chosen as the activity to add" in new Fixture {
        val model = AddServiceFlowModel(
          Some(BusinessActivities(Set(BillPaymentServices))),
          Some(NewActivitiesAtTradingPremisesYes(BillPaymentServices)))

        val result = routingFile.getRoute(AddServiceSummaryPageId, model)

        result mustBe Redirect(controllers.routes.RegistrationProgressController.get())
      }
    }


    "return the 'registration progress' page" when {
      "we're on the 'new service information' page" in new Fixture {
        val model = AddServiceFlowModel(
          Some(BusinessActivities(Set(HighValueDealing))),
          Some(NewActivitiesAtTradingPremisesYes(HighValueDealing)))

        val result = routingFile.getRoute(NewServiceInformationPageId, model)

        result mustBe Redirect(controllers.routes.RegistrationProgressController.get())
      }
    }

  }

}
