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


import models.businessmatching._
import models.flowmanagement._
import org.scalatestplus.play.PlaySpec
import controllers.businessmatching.updateservice.remove.{routes => removeRoutes}
import services.flowmanagement.pagerouters.removeflow._
import play.api.mvc.Results.Redirect
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService

import utils.{AmlsSpec, DependencyMocks}

class RemoveBusinessTypeRouterSpec extends PlaySpec {

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.Future

  class VariationRemoveServiceRouterSpec extends AmlsSpec {

    trait Fixture extends DependencyMocks {

      val mockBusinessMatchingService = mock[BusinessMatchingService]

      val router = new RemoveBusinessTypeRouter(
        businessMatchingService = mockBusinessMatchingService,
        whatServicesToRemovePageRouter = new WhatBusinessTypesToRemovePageRouter(mockStatusService, mockBusinessMatchingService),
        needToUpdatePageRouter = new NeedToUpdatePageRouter(mockStatusService, mockBusinessMatchingService),
        removeServicesSummaryPageRouter = new RemoveBusinessTypesSummaryPageRouter(mockStatusService, mockBusinessMatchingService),
        unableToRemovePageRouter = new UnableToRemovePageRouter(mockStatusService, mockBusinessMatchingService),
        whatDateToRemovePageRouter = new WhatDateToRemovePageRouter(mockStatusService, mockBusinessMatchingService)
      )
    }

    "getRoute" must {

      "return the 'What do you want to remove' page (RemoveActivitiesController)" when {
        "the user is on the 'What do you want to do' page (ChangeServicesPageId)" when {
          "there is more than one business type in the model" in new Fixture {

            val model = RemoveBusinessTypeFlowModel(
              activitiesToRemove = Some(Set(HighValueDealing, MoneyServiceBusiness))
            )
            val result = await(router.getRoute(ChangeServicesPageId, model))

            result mustBe Redirect(removeRoutes.RemoveBusinessTypesController.get())
          }
        }
      }

      "return the 'Unable to remove' page (UnableToRemoveBusinessTypesController)" when {
        "the user is on the 'What do you want to do' page (ChangeServicesPageId)" when {
          "there is less than two business type in the model" in new Fixture {

            val model = RemoveBusinessTypeFlowModel(
              activitiesToRemove = Some(Set(HighValueDealing))
            )
            val result = await(router.getRoute(ChangeServicesPageId, model))

            result mustBe Redirect(removeRoutes.UnableToRemoveBusinessTypesController.get())
          }
        }
      }

      "return the 'status' page (UnableToRemoveActivitiesController)" when {
        "the user is on the 'Unable to remove' page (UnableToRemovePageId)" when {
          "there is less than two business type in the model" in new Fixture {

            val model = RemoveBusinessTypeFlowModel(
              activitiesToRemove = Some(Set(HighValueDealing))
            )
            val result = await(router.getRoute(UnableToRemovePageId, model))

            result mustBe Redirect(controllers.routes.StatusController.get())
          }
        }
      }

      "return the 'Date of change' page (UpdateServiceDateOfChangeController)" when {
        "the user is on the 'What do you want to remove' page (WhatServiceToRemovePageId)" when {
          "there is less than two business type in the model" in new Fixture {

            val model = RemoveBusinessTypeFlowModel(
              activitiesToRemove = Some(Set(HighValueDealing, MoneyServiceBusiness))
            )
            val result = await(router.getRoute(WhatBusinessTypesToRemovePageId, model))

            result mustBe Redirect(removeRoutes.WhatDateRemovedController.get())
          }
        }
      }
    }
  }
}
