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


import controllers.businessmatching.updateservice.RemoveBusinessTypeHelper
import controllers.businessmatching.updateservice.remove.{routes => removeRoutes}
import models.DateOfChange
import models.businessmatching._
import models.businessmatching.updateservice.ServiceChangeRegister
import models.flowmanagement._
import org.joda.time.LocalDate
import play.api.mvc.Results.Redirect
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.pagerouters.removeflow._
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

import scala.concurrent.ExecutionContext.Implicits.global


class RemoveBusinessTypeRouterSpec extends AmlsSpec {

  trait Fixture extends DependencyMocks with AuthorisedFixture {
    self =>

    val mockBusinessMatchingService = mock[BusinessMatchingService]

    val removeBusinessTypeHelper = new RemoveBusinessTypeHelper(
      self.authConnector,
      mockCacheConnector
    )

    val router = new RemoveBusinessTypeRouter(
      businessMatchingService = mockBusinessMatchingService,
      whatServicesToRemovePageRouter = new WhatBusinessTypesToRemovePageRouter(mockStatusService, mockBusinessMatchingService, removeBusinessTypeHelper = removeBusinessTypeHelper),
      needToUpdatePageRouter = new NeedToUpdatePageRouter(),
      removeServicesSummaryPageRouter = new RemoveBusinessTypesSummaryPageRouter(mockStatusService, mockBusinessMatchingService),
      unableToRemovePageRouter = new UnableToRemovePageRouter(mockStatusService, mockBusinessMatchingService),
      whatDateToRemovePageRouter = new WhatDateToRemovePageRouter(mockStatusService, mockBusinessMatchingService)
    )
  }

  "getRoute" must {

    //What do you want to remove
    "return the 'Date of change' page (WhatDateRemovedController)" when {
      "the user is on the 'What do you want to remove' page (WhatBusinessTypesToRemovePageId)" when {

        "all of the business types to remove have not been submitted" in new Fixture {

          val model = RemoveBusinessTypeFlowModel(
            activitiesToRemove = Some(Set(HighValueDealing, MoneyServiceBusiness))
          )

          val justAdded = ServiceChangeRegister(addedActivities = Some(Set(HighValueDealing, MoneyServiceBusiness)))

          mockCacheFetch[ServiceChangeRegister](
            Some(justAdded),
            Some(ServiceChangeRegister.key))

          val result = await(router.getRoute(WhatBusinessTypesToRemovePageId, model))

          result mustBe Redirect(removeRoutes.RemoveBusinessTypesSummaryController.get())
        }
      }
    }

    "return the 'check your answers' page (RemoveBusinessTypesSummaryController)" when {
      "the user is on the 'What do you want to remove' page (WhatBusinessTypesToRemovePageId)" when {
        "some of the business types have not been submitted" in new Fixture {
          val model = RemoveBusinessTypeFlowModel(
            activitiesToRemove = Some(Set(HighValueDealing, MoneyServiceBusiness))
          )

          val justAdded = ServiceChangeRegister(addedActivities = Some(Set(TrustAndCompanyServices)))

          mockCacheFetch[ServiceChangeRegister](
            Some(justAdded),
            Some(ServiceChangeRegister.key))
          val result = await(router.getRoute(WhatBusinessTypesToRemovePageId, model))

          result mustBe Redirect(removeRoutes.WhatDateRemovedController.get())
        }
        "some of the business types have not been submitted and in edit mode" in new Fixture {
          val model = RemoveBusinessTypeFlowModel(
            activitiesToRemove = Some(Set(HighValueDealing, MoneyServiceBusiness))
          )

          val justAdded = ServiceChangeRegister(addedActivities = Some(Set(TrustAndCompanyServices)))

          mockCacheFetch[ServiceChangeRegister](
            Some(justAdded),
            Some(ServiceChangeRegister.key))
          val result = await(router.getRoute(WhatBusinessTypesToRemovePageId, model, true))

          result mustBe Redirect(removeRoutes.WhatDateRemovedController.get())
        }
      }
    }

    //Date of change
    "return the 'check your answers' page (RemoveBusinessTypesSummaryController)" when {
      "the user is on the 'Date of change' page (WhatDateRemovedPageId)" when {
        "there is more than one business type in the model" in new Fixture {

          val model = RemoveBusinessTypeFlowModel(
            activitiesToRemove = Some(Set(HighValueDealing, MoneyServiceBusiness)),
            dateOfChange = Some(DateOfChange(LocalDate.now()))
          )
          val result = await(router.getRoute(WhatDateRemovedPageId, model))

          result mustBe Redirect(removeRoutes.RemoveBusinessTypesSummaryController.get())
        }
      }
    }

    //check your answers (2 options - option 1)
    "return the 'progress' page (RegistrationProgressController)" when {
      "the user is on the 'check your answers' page (RemoveBusinessTypesSummaryPageId)" when {
        "there is no ASP business type in the model" in new Fixture {

          val model = RemoveBusinessTypeFlowModel(
            activitiesToRemove = Some(Set(HighValueDealing, MoneyServiceBusiness)),
            dateOfChange = Some(DateOfChange(LocalDate.now()))
          )
          val result = await(router.getRoute(RemoveBusinessTypesSummaryPageId, model))

          result mustBe Redirect(controllers.routes.RegistrationProgressController.get())
        }
      }
    }

    //check your answers (2 options - option 2)
    "return the 'Need to update Answers' page (NeedToUpdateController)" when {
      "the user is on the 'check your answers' page (RemoveBusinessTypesSummaryPageId)" when {
        "there is an ASP business type in the model" in new Fixture {

          val model = RemoveBusinessTypeFlowModel(
            activitiesToRemove = Some(Set(HighValueDealing, MoneyServiceBusiness, AccountancyServices)),
            dateOfChange = Some(DateOfChange(LocalDate.now()))
          )
          val result = await(router.getRoute(RemoveBusinessTypesSummaryPageId, model))

          result mustBe Redirect(removeRoutes.NeedMoreInformationController.get())
        }
      }
    }

    //Need to update answers
    "return the 'progress' page (RegistrationProgressController)" when {
      "the user is on the 'Need to update Answers' page (NeedToUpdatePageId)" when {
        "there is an ASP business type in the model" in new Fixture {

          val model = RemoveBusinessTypeFlowModel(
            activitiesToRemove = Some(Set(HighValueDealing, MoneyServiceBusiness, AccountancyServices)),
            dateOfChange = Some(DateOfChange(LocalDate.now()))
          )
          val result = await(router.getRoute(NeedToUpdatePageId, model))

          result mustBe Redirect(controllers.routes.RegistrationProgressController.get())
        }
      }
    }

    //Unable to remove
    "return the 'status' page (StatusController)" when {
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

    //***********************************    Edit tests  *******************************************************************

    //edit Date of change
    "return the 'Check your answers' page (RemoveBusinessTypesSummaryController)" when {
      "editing the 'date of change' page (WhatDateRemovedPageId)" in new Fixture {

        val model = RemoveBusinessTypeFlowModel(
          activitiesToRemove = Some(Set(HighValueDealing, MoneyServiceBusiness)),
          dateOfChange = Some(DateOfChange(LocalDate.now()))
        )

        val result = await(router.getRoute(WhatDateRemovedPageId, model, edit = true))

        result mustBe Redirect(removeRoutes.RemoveBusinessTypesSummaryController.get())
      }
    }
  }
}