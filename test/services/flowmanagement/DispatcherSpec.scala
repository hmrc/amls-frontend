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

package services.flowmanagement

import models.businessmatching.{BusinessActivities, HighValueDealing}
import models.businessmatching.updateservice.{AreNewActivitiesAtTradingPremises, ChangeServices, ChangeServicesAdd, NewActivitiesAtTradingPremisesYes}
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Results.Redirect

class DispatcherSpec extends PlaySpec {

  trait Fixture {
    val dispatcher = new Dispatcher()

  }

  "getRoute" must {
    "return the services view page" when {
      "given the 'what do you want to do' model and 'add' has been selected" in new Fixture {
        val model = ChangeServicesAdd
        val result = dispatcher.getRoute(model, VariationAddServiceRouting)

        result mustBe Redirect(controllers.businessmatching.updateservice.routes.ChangeServicesController.get())

      }
    }

    "return the 'trading premises' view page" when {
      "given the 'which services' model" in new Fixture {
        val model = BusinessActivities(Set(HighValueDealing))
        val result = dispatcher.getRoute(model, VariationAddServiceRouting)

        result mustBe Redirect(controllers.businessmatching.updateservice.routes.TradingPremisesController.get(0))
      }
    }

    "return the 'which trading premises' view page" when {
      "given the 'trading premises yes' model" in new Fixture {
        val model = NewActivitiesAtTradingPremisesYes(HighValueDealing)
        val result = dispatcher.getRoute(model, VariationAddServiceFlow)

        result mustBe Redirect(controllers.businessmatching.updateservice.routes.WhichTradingPremisesController.get(0))
      }
    }
  }

}
