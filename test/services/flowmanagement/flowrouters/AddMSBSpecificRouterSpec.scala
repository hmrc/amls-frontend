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
import controllers.{routes => rootRoutes}
import models.businessmatching._
import models.businessmatching.BusinessActivity._
import models.businessmatching.BusinessMatchingMsbService._
import models.flowmanagement._
import play.api.mvc.Results.Redirect
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.flowrouters.businessmatching.AddBusinessTypeRouter
import services.flowmanagement.pagerouters.addflow._
import utils.{AmlsSpec, DependencyMocks}

class AddMSBSpecificRouterSpec extends AmlsSpec {

  val model = AddBusinessTypeFlowModel(activity = Some(MoneyServiceBusiness))

  trait Fixture extends DependencyMocks {

    val mockBusinessMatchingService = mock[BusinessMatchingService]

    val router = new AddBusinessTypeRouter(
      businessMatchingService = mockBusinessMatchingService,
      addMoreActivitiesPageRouter = new AddMoreBusinessTypesPageRouter(mockStatusService, mockBusinessMatchingService),
      businessAppliedForFrnPageRouter =
        new BusinessAppliedForFrnPageRouter(mockStatusService, mockBusinessMatchingService),
      newServicesInformationPageRouter = new NeedMoreInformationPageRouter(),
      noFrnPageRouter = new NoFrnPageRouter(mockStatusService, mockBusinessMatchingService),
      selectActivitiesPageRouter = new SelectBusinessTypesPageRouter(mockStatusService, mockBusinessMatchingService),
      subServicesPageRouter = new SubSectorsPageRouter(mockStatusService, mockBusinessMatchingService),
      updateServicesSummaryPageRouter =
        new AddBusinessTypeSummaryPageRouter(mockStatusService, mockBusinessMatchingService)
    )
  }

  "In the Add MSB flow the getRoute method" must {

    // Start MSB sub-flow
    "return the 'msb_subservice' page (SubServiceController)" when {
      "the user is on the 'What Type of business ....' page (SelectActivitiesPageId)" when {
        "MSB is selected" in new Fixture {

          val result = await(router.getRoute("internalId", SelectBusinessTypesPageId, model))

          result mustBe Redirect(addRoutes.SubSectorsController.get(false))
        }
      }
    }

    "return the 'frn_number' page (BusinessAppliedForFrnController)" when {
      "the user is on the 'msb_subservice' page (SubServicePageId)" when {
        "MSB is the Business Activity and subservices contains TransmittingMoney" in new Fixture {
          val model  = AddBusinessTypeFlowModel(
            activity = Some(MoneyServiceBusiness),
            subSectors = Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))
          )
          val result = await(router.getRoute("internalId", SubSectorsPageId, model))

          result mustBe Redirect(addRoutes.BusinessAppliedForFrnController.get(false))
        }
      }
    }

    "return the 'update_services_summary' page (AddBusinessTypeSummaryController)" when {
      "the user is on the 'msb_subservice' page (SubServicePageId)" when {
        "MSB is the Business Activity and subservices does not contain TransmittingMoney" in new Fixture {
          val model  = AddBusinessTypeFlowModel(
            activity = Some(MoneyServiceBusiness),
            subSectors = Some(BusinessMatchingMsbServices(Set(ChequeCashingScrapMetal, ChequeCashingNotScrapMetal)))
          )
          val result = await(router.getRoute("internalId", SubSectorsPageId, model))

          result mustBe Redirect(addRoutes.AddBusinessTypeSummaryController.get())
        }
      }
    }

    "return the 'no-psr' page (NoFrnController)" when {
      "the user is on the 'psr_number' page (BusinessAppliedForPSRNumberPageId)" when {
        "the answer is no and MSB is the Business Activity" in new Fixture {
          val model = AddBusinessTypeFlowModel(
            activity = Some(MoneyServiceBusiness),
            businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberNo)
          )

          val result = await(router.getRoute("internalId", FrnPageId, model))

          result mustBe Redirect(addRoutes.NoFrnController.get())
        }
      }
    }

    "return the 'update_services_summary' page (AddBusinessTypeSummaryController)" when {
      "the user is on the 'psr_number' page (BusinessAppliedForPSRNumberPageId)" when {
        "the answer is yes and MSB is the Business Activity" in new Fixture {
          val model = AddBusinessTypeFlowModel(
            activity = Some(MoneyServiceBusiness),
            businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("aaaaa"))
          )

          val result = await(router.getRoute("internalId", FrnPageId, model))

          result mustBe Redirect(addRoutes.AddBusinessTypeSummaryController.get())
        }
      }
    }

    "return the 'About your business' page (RegistrationProgressController)" when {
      "the user is on the 'no_psr' page (NoFrnPageId)" when {
        "MSB is the Business Activity" in new Fixture {
          val result = await(router.getRoute("internalId", NoFrnPageId, model))

          result mustBe Redirect(rootRoutes.RegistrationProgressController.get())
        }
      }
    }

    "return the 'update_services_summary' page (AddBusinessTypeSummaryController)" when {
      "the user is on the 'subsectors page' page (SubSectorsPageId)" when {
        "MSB is the Business Activity" in new Fixture {
          val result = await(router.getRoute("internalId", SubSectorsPageId, model))

          result mustBe Redirect(addRoutes.AddBusinessTypeSummaryController.get())
        }
      }
    }
  }

  // Edit mode MSB sub-flow
  "When Editing in the MSB Add flow, the getRoute method" must {

    // Edit Subservices
    "return the 'CYA' page (AddBusinessTypeSummaryController)" when {
      "the user is on the 'msb_subservice' page (SubServicePageId)" when {
        "MSB is the Business Activity and subservices contains TransmittingMoney and PSRNumber" in new Fixture {
          val model  = AddBusinessTypeFlowModel(
            activity = Some(MoneyServiceBusiness),
            subSectors = Some(BusinessMatchingMsbServices(Set(TransmittingMoney, ChequeCashingScrapMetal))),
            businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("aaaaa"))
          )
          val result = await(router.getRoute("internalId", SubSectorsPageId, model, edit = true))

          result mustBe Redirect(addRoutes.AddBusinessTypeSummaryController.get())
        }
      }
    }

    "return the 'PSR Number' page (BusinessAppliedForFrnController)" when {
      "the user is on the 'msb_subservice' page (SubServicePageId)" when {
        "MSB is the Business Activity and subservices contains TransmittingMoney and PSR is not defined" in new Fixture {
          val model  = AddBusinessTypeFlowModel(
            activity = Some(MoneyServiceBusiness),
            subSectors = Some(BusinessMatchingMsbServices(Set(TransmittingMoney, ChequeCashingScrapMetal)))
          )
          val result = await(router.getRoute("internalId", SubSectorsPageId, model, edit = true))

          result mustBe Redirect(addRoutes.BusinessAppliedForFrnController.get(true))
        }
      }
    }

    "return the 'CYA' page (AddBusinessTypeSummaryController)" when {
      "the user is on the 'msb_subservice' page (SubServicePageId)" when {
        "MSB is the Business Activity and subservices does not contain TransmittingMoney" in new Fixture {
          val model  = AddBusinessTypeFlowModel(
            activity = Some(MoneyServiceBusiness),
            subSectors = Some(BusinessMatchingMsbServices(Set(CurrencyExchange, ChequeCashingScrapMetal)))
          )
          val result = await(router.getRoute("internalId", SubSectorsPageId, model, edit = true))

          result mustBe Redirect(addRoutes.AddBusinessTypeSummaryController.get())
        }
      }
    }

    "return the 'Check your answers' page (AddBusinessTypeSummaryController)" when {
      "the user is on the 'msb_subservice' page (SubServicePageId)" when {
        "MSB is the Business Activity and subservices does contains TransmittingMoney, PSR is defined" in new Fixture {
          val model  = AddBusinessTypeFlowModel(
            activity = Some(MoneyServiceBusiness),
            businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("bbbbb")),
            subSectors = Some(BusinessMatchingMsbServices(Set(TransmittingMoney, ChequeCashingScrapMetal)))
          )
          val result = await(router.getRoute("internalId", SubSectorsPageId, model, edit = true))

          result mustBe Redirect(addRoutes.AddBusinessTypeSummaryController.get())
        }
      }
    }

    "return the 'CYA' page (AddBusinessTypeSummaryController)" when {
      "the user is on the 'msb_subservice' page (SubServicePageId)" when {
        "MSB is the Business Activity and subservices does contains TransmittingMoney, PSR is defined" in new Fixture {
          val model  = AddBusinessTypeFlowModel(
            activity = Some(MoneyServiceBusiness),
            businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("bbbbb")),
            subSectors = Some(BusinessMatchingMsbServices(Set(TransmittingMoney, ChequeCashingScrapMetal)))
          )
          val result = await(router.getRoute("internalId", SubSectorsPageId, model, edit = true))

          result mustBe Redirect(addRoutes.AddBusinessTypeSummaryController.get())
        }
      }
    }

    "return the 'Check Your Answers' page (AddBusinessTypeSummaryController)" when {
      "the user is on the 'msb_subservice' page (SubServicePageId)" when {
        "MSB is the Business Activity and subservices is TransmittingMoney and PSRNumber is defined" in new Fixture {
          val model  = AddBusinessTypeFlowModel(
            activity = Some(MoneyServiceBusiness),
            subSectors = Some(BusinessMatchingMsbServices(Set(TransmittingMoney))),
            businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("aaaaa"))
          )
          val result = await(router.getRoute("internalId", SubSectorsPageId, model, edit = true))

          result mustBe Redirect(addRoutes.AddBusinessTypeSummaryController.get())
        }
      }
    }

    "return the 'Check Your Answers' page (AddBusinessTypeSummaryController)" when {
      "the user is on the 'msb_subservice' page (SubServicePageId)" when {
        "MSB is the Business Activity and subservices is size 1" in new Fixture {
          val model  = AddBusinessTypeFlowModel(
            activity = Some(MoneyServiceBusiness),
            subSectors = Some(BusinessMatchingMsbServices(Set(CurrencyExchange)))
          )
          val result = await(router.getRoute("internalId", SubSectorsPageId, model, edit = true))

          result mustBe Redirect(addRoutes.AddBusinessTypeSummaryController.get())
        }
      }
    }

    "return the 'Business Applied For FRN' page (BusinessAppliedForFrn)" when {
      "the user is on the 'msb_subservice' page (SubServicePageId)" when {
        "MSB is the Business Activity and subservices is TransmittingMoney and FRN is not defined" in new Fixture {
          val model  = AddBusinessTypeFlowModel(
            activity = Some(MoneyServiceBusiness),
            subSectors = Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))
          )
          val result = await(router.getRoute("internalId", SubSectorsPageId, model, edit = true))

          result mustBe Redirect(addRoutes.BusinessAppliedForFrnController.get(true))
        }
      }
    }

    "return the 'Business Applied For FRN' page (BusinessAppliedForFrn)" when {
      "the user is on the 'msb_subservice' page (SubServicePageId)" when {
        "MSB is the Business Activity and subservices is TransmittingMoney and FRN is None" in new Fixture {
          val model  = AddBusinessTypeFlowModel(
            activity = Some(MoneyServiceBusiness),
            businessAppliedForPSRNumber = None,
            subSectors = Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))
          )
          val result = await(router.getRoute("internalId", SubSectorsPageId, model, edit = true))

          result mustBe Redirect(addRoutes.BusinessAppliedForFrnController.get(true))
        }
      }
    }

    // edit Business Applied For PSR Number true
    "return the 'update_services_summary' page (AddBusinessTypeSummaryController)" when {
      "editing the 'Business FRN' page (BusinessAppliedForPSRNumberPageId)" when {
        "the answer is yes" in new Fixture {
          val model  = AddBusinessTypeFlowModel(
            activity = Some(MoneyServiceBusiness),
            businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("bbbbb")),
            subSectors = Some(BusinessMatchingMsbServices(Set(TransmittingMoney, ChequeCashingScrapMetal)))
          )
          val result = await(router.getRoute("internalId", FrnPageId, model, edit = true))

          result mustBe Redirect(addRoutes.AddBusinessTypeSummaryController.get())
        }
      }
    }

    // edit Business Applied For PSR Number false
    "return the 'no-psr' page (NoFrnController)" when {
      "the user is on the 'psr_number' page (BusinessAppliedForPSRNumberPageId)" when {
        "the answer is yes" in new Fixture {
          val model  = AddBusinessTypeFlowModel(
            activity = Some(MoneyServiceBusiness),
            businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberNo)
          )
          val result = await(router.getRoute("internalId", FrnPageId, model))

          result mustBe Redirect(addRoutes.NoFrnController.get())
        }
      }
    }
  }
}
