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

package services.flowmanagement.pagerouters.businessmatching.subsectors

import controllers.businessmatching.routes
import models.businessmatching.BusinessMatchingMsbService.TransmittingMoney
import models.businessmatching.{BusinessAppliedForFrn, BusinessAppliedForPSRNumberNo, BusinessAppliedForFrnYes}
import models.flowmanagement.ChangeSubSectorFlowModel
import utils.AmlsSpec
import play.api.test.Helpers._

class FrnPageRouterSpec extends AmlsSpec {

  trait Fixture {
    val router = new FrnPageRouter

    val createModel: Option[BusinessAppliedForFrn] => ChangeSubSectorFlowModel =
      ChangeSubSectorFlowModel.apply(Some(Set(TransmittingMoney)), _)
  }

  trait NotRegisteredFixture {
    val router = new FrnPageRouterCompanyNotRegistered

    val createModel: Option[BusinessAppliedForFrn] => ChangeSubSectorFlowModel =
      ChangeSubSectorFlowModel.apply(Some(Set(TransmittingMoney)), _)
  }

  "FrnPageRouter" must {
    "redirect to the 'check your answers' page" when {
      "the user has entered a FRN" in new Fixture {
        val model  = createModel(Some(BusinessAppliedForFrnYes("123456789")))
        val result = router.getRoute("internalId", model)

        redirectLocation(result) mustBe Some(routes.SummaryController.get().url)
      }
    }

    "route to the 'you can't continue with your change' page" when {
      "there is no FRN" in new Fixture {
        val model  = createModel(Some(BusinessAppliedForPSRNumberNo))
        val result = router.getRoute("internalId", model)

        redirectLocation(result) mustBe Some(routes.NoFrnController.get.url)
      }
    }

    "return an Internal Server Error" when {
      "there is no FRN data" in new Fixture {
        val model  = createModel(None)
        val result = router.getRoute("internalId", model)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "FrnPageRouterCompanyNotRegistered" must {
    "redirect to the 'check your answers' page" when {
      "the user has entered a FRN and includeCompanyNotRegistered is false" in new NotRegisteredFixture {
        val model  = createModel(Some(BusinessAppliedForFrnYes("123456789")))
        val result = router.getRoute("internalId", model)

        redirectLocation(result) mustBe Some(routes.SummaryController.get().url)
      }
    }

    "redirect to the 'check company' page" when {
      "the user has entered a FRN and includeCompanyNotRegistered is true" in new NotRegisteredFixture {
        val model  = createModel(Some(BusinessAppliedForFrnYes("123456789")))
        val result = router.getRoute("internalId", model, includeCompanyNotRegistered = true)

        redirectLocation(result) mustBe Some(routes.CheckCompanyController.get().url)
      }
    }

    "route to the 'you can't continue with your change' page" when {
      "there is no FRN" in new NotRegisteredFixture {
        val model  = createModel(Some(BusinessAppliedForPSRNumberNo))
        val result = router.getRoute("internalId", model)

        redirectLocation(result) mustBe Some(routes.NoFrnController.get.url)
      }
    }

    "return an Internal Server Error" when {
      "there is no FRN" in new NotRegisteredFixture {
        val model  = createModel(None)
        val result = router.getRoute("internalId", model)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

}
