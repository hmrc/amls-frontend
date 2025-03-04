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

import controllers.businessmatching.updateservice.add.{routes => addRoutes}
import models.businessmatching.BusinessActivity._
import models.flowmanagement._
import play.api.mvc.Results.Redirect
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.flowrouters.businessmatching.AddBusinessTypeRouter
import services.flowmanagement.pagerouters.addflow._

import utils.{AmlsSpec, DependencyMocks}

class AddTCSPSpecificRouterSpec extends AmlsSpec {

  val model = AddBusinessTypeFlowModel(activity = Some(TrustAndCompanyServices))

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

  "In the Add TCSP flow the getRoute method" must {
    "return the 'update_services_summary' page (AddBusinessTypeSummaryController)" when {
      "the user is on the 'What Type of business ....' page (SelectActivitiesPageId)" when {
        "TCSP is selected" in new Fixture {

          val result = await(router.getRoute("internalId", SelectBusinessTypesPageId, model))

          result mustBe Redirect(addRoutes.AddBusinessTypeSummaryController.get())
        }
      }
    }
  }

  "When Editing in the TCSP Add flow, the getRoute method" must {
    "return the 'update_services_summary' page (AddBusinessTypeSummaryController)" when {
      "editing the 'Business types' page (SelectActivitiesPageId)" when {
        "selection is TCSP" in new Fixture {
          val model  = AddBusinessTypeFlowModel(activity = Some(TrustAndCompanyServices))
          val result = await(router.getRoute("internalId", SelectBusinessTypesPageId, model, edit = true))

          result mustBe Redirect(addRoutes.AddBusinessTypeSummaryController.get())
        }
      }
    }
  }
}
