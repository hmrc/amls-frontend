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

import config.ApplicationConfig
import controllers.businessmatching.updateservice.RemoveBusinessTypeHelper
import controllers.businessmatching.updateservice.remove.{routes => removeRoutes}
import generators.tradingpremises.TradingPremisesGenerator
import models.DateOfChange
import models.businessmatching.BusinessActivity._
import models.businessmatching.updateservice.ServiceChangeRegister
import models.flowmanagement._
import models.tradingpremises.TradingPremises
import org.scalacheck.Gen
import play.api.mvc.Results.Redirect
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.flowrouters.businessmatching.RemoveBusinessTypeRouter
import services.flowmanagement.pagerouters.removeflow._
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

import java.time.LocalDate

class RemoveBusinessTypeRouterSpec extends AmlsSpec with TradingPremisesGenerator {

  trait Fixture extends DependencyMocks with AuthorisedFixture {
    self =>

    val mockBusinessMatchingService = mock[BusinessMatchingService]
    val mockApplicationConfig = mock[ApplicationConfig]

    val removeBusinessTypeHelper = new RemoveBusinessTypeHelper()(mockCacheConnector)

    val router = new RemoveBusinessTypeRouter(
      businessMatchingService = mockBusinessMatchingService,
      whatServicesToRemovePageRouter = new WhatBusinessTypesToRemovePageRouter(mockStatusService, mockBusinessMatchingService, removeBusinessTypeHelper = removeBusinessTypeHelper),
      needToUpdatePageRouter = new NeedToUpdatePageRouter(),
      removeServicesSummaryPageRouter = new RemoveBusinessTypesSummaryPageRouter(mockStatusService, mockBusinessMatchingService, mockCacheConnector),
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

          val result = await(router.getRoute("internalId", WhatBusinessTypesToRemovePageId, model))

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
          val result = await(router.getRoute("internalId", WhatBusinessTypesToRemovePageId, model))

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
          val result = await(router.getRoute("internalId", WhatBusinessTypesToRemovePageId, model, true))

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
          val result = await(router.getRoute("internalId", WhatDateRemovedPageId, model))

          result mustBe Redirect(removeRoutes.RemoveBusinessTypesSummaryController.get())
        }
      }
    }

    //check your answers (2 options - option 1)
    "return the 'Need to update Answers' page (NeedToUpdateController)" when {
      "the user is on the 'check your answers' page (RemoveBusinessTypesSummaryPageId)" when {
        "there is no ASP business type in the model" in new Fixture {

          mockCacheFetch[Seq[TradingPremises]](Some(Gen.listOfN(2, tradingPremisesWithAtLeastOneBusinessTypeGen).sample.get))

          val model = RemoveBusinessTypeFlowModel(
            activitiesToRemove = Some(Set(HighValueDealing, MoneyServiceBusiness)),
            dateOfChange = Some(DateOfChange(LocalDate.now()))
          )

          val result = await(router.getRoute("internalId", RemoveBusinessTypesSummaryPageId, model))

          result mustBe Redirect(removeRoutes.NeedMoreInformationController.get())
        }
      }
    }

    //check your answers (2 options - option 2)
    "return the 'Need to update Answers' page (NeedToUpdateController)" when {
      "the user is on the 'check your answers' page (RemoveBusinessTypesSummaryPageId)" when {
        "there is an ASP business type in the model" in new Fixture {
          mockCacheFetch[Seq[TradingPremises]](Some(Gen.listOfN(2, tradingPremisesWithAtLeastOneBusinessTypeGen).sample.get))

          val model = RemoveBusinessTypeFlowModel(
            activitiesToRemove = Some(Set(HighValueDealing, MoneyServiceBusiness, AccountancyServices)),
            dateOfChange = Some(DateOfChange(LocalDate.now()))
          )
          val result = await(router.getRoute("internalId", RemoveBusinessTypesSummaryPageId, model))

          result mustBe Redirect(removeRoutes.NeedMoreInformationController.get())
        }

        "there are incomplete trading premises in the data" in new Fixture {

          mockCacheFetch[Seq[TradingPremises]](Some(Gen.listOfN(4, tradingPremisesGen).sample.get map { tp => tp.copy(hasAccepted = false) }))

          val model = RemoveBusinessTypeFlowModel(
            activitiesToRemove = Some(Set(HighValueDealing)),
            dateOfChange = Some(DateOfChange(LocalDate.now()))
          )

          val result = await(router.getRoute("internalId", RemoveBusinessTypesSummaryPageId, model))

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
          val result = await(router.getRoute("internalId", NeedToUpdatePageId, model))

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
          val result = await(router.getRoute("internalId", UnableToRemovePageId, model))

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

        val result = await(router.getRoute("internalId", WhatDateRemovedPageId, model, edit = true))

        result mustBe Redirect(removeRoutes.RemoveBusinessTypesSummaryController.get())
      }
    }

    "return the 'Check your answers' page (RemoveBusinessTypesSummaryController)" when {
      "editing the 'what business types to remove' page (WhatDateRemovedPageId) and the date of change exists" in new Fixture {

        val model = RemoveBusinessTypeFlowModel(
          activitiesToRemove = Some(Set(HighValueDealing, MoneyServiceBusiness)),
          dateOfChange = Some(DateOfChange(LocalDate.now()))
        )

        val result = await(router.getRoute("internalId", WhatBusinessTypesToRemovePageId, model, edit = true))

        result mustBe Redirect(removeRoutes.RemoveBusinessTypesSummaryController.get())
      }
    }
  }
}