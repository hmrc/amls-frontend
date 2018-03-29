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

import models.businessmatching.updateservice.{ChangeServicesAdd, NewActivitiesAtTradingPremisesNo, NewActivitiesAtTradingPremisesYes, TradingPremisesActivities}
import models.businessmatching.{BillPaymentServices, BusinessActivities, HighValueDealing}
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Results.Redirect
import services.flowmanagement.routings.VariationAddServiceRouting


class VariationAddServiceRoutingSpec extends PlaySpec {

  trait Fixture {

    val routingFile = VariationAddServiceRouting

  }

  "getRoute" must {
    "return the services page" when {
      "given the 'ChangeServicesAdd' model " in new Fixture {
        val result = routingFile.getRoute(ChangeServicesAdd)

        result mustBe Redirect(controllers.businessmatching.updateservice.routes.ChangeServicesController.get())
      }
    }

    "return the 'trading premises' page" when {
      "given the 'BusinessActivities' model contains a single activity" in new Fixture {
        val model = BusinessActivities(Set(HighValueDealing))
        val result = routingFile.getRoute(model)

        result mustBe Redirect(controllers.businessmatching.updateservice.routes.TradingPremisesController.get(0))
      }
    }

    "return the 'Check your answers' page" when {
      "given the 'TradingPremisesActivities' model contains a single trading premises" in new Fixture {
        val model = TradingPremisesActivities(Set(0))
        val result = routingFile.getRoute(model)

        result mustBe Redirect(controllers.businessmatching.updateservice.routes.UpdateServicesSummaryController.get())
      }
    }

    "return the 'Check your answers' page" when {
      "given the 'TradingPremisesActivities' model contains a multiple trading premises" in new Fixture {
        val model = TradingPremisesActivities(Set(0, 1))
        val result = routingFile.getRoute(model)

        result mustBe Redirect(controllers.businessmatching.updateservice.routes.UpdateServicesSummaryController.get())
      }
    }

    "return the 'which trading premises' page" when {
      "given the 'NewActivitiesAtTradingPremisesYes' model contains HVD" in new Fixture {
        val model = NewActivitiesAtTradingPremisesYes(HighValueDealing)
        val result = routingFile.getRoute(model)

        result mustBe Redirect(controllers.businessmatching.updateservice.routes.WhichTradingPremisesController.get(0))
      }
    }

    "return the 'Check your answers' page" when {
      "given the 'NewActivitiesAtTradingPremisesNo' model contains no activities " in new Fixture {
        val model = NewActivitiesAtTradingPremisesNo
        val result = routingFile.getRoute(model)

        result mustBe Redirect(controllers.businessmatching.updateservice.routes.UpdateServicesSummaryController.get())
      }
    }
  }

}
