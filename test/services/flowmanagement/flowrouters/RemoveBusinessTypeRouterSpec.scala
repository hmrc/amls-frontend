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

import org.scalatestplus.play.PlaySpec
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.pagerouters.removeflow._
import utils.DependencyMocks

class RemoveBusinessTypeRouterSpec extends PlaySpec {

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
        "there is more than one businesstype in the model" in new Fixture {

          //        val model = AddServiceFlowModel(
          //          activity = Some(HighValueDealing))
          //        val result = await(router.getRoute(SelectActivitiesPageId, model))

          //result mustBe Redirect(addRoutes.TradingPremisesController.get())
        }
      }
    }

  }
}
