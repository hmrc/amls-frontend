/*
 * Copyright 2026 HM Revenue & Customs
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
import models.businessmatching.{BusinessAppliedForFrn, BusinessAppliedForFrnNo, BusinessAppliedForFrnYes}
import models.flowmanagement.ChangeSubSectorFlowModel
import play.api.test.Helpers.*
import utils.AmlsSpec

class FrnNeedMoreInfoPageRouterSpec extends AmlsSpec {

  trait Fixture {
    val router = new FrnNeedMoreInfoPageRouter

    val createModel: Option[BusinessAppliedForFrn] => ChangeSubSectorFlowModel =
      ChangeSubSectorFlowModel.apply(Some(Set(TransmittingMoney)), _)
  }

  "FrnNeedMoreInfoPageRouter" must {
    "Go to the 'check your answers' page" when {
      "the includeCompanyNotRegistered is false" in new Fixture {
        val model  = createModel(Some(BusinessAppliedForFrnYes("1234567")))
        val result = router.getRoute("internalId", model, includeCompanyNotRegistered = false)

        redirectLocation(result) mustBe Some(routes.SummaryController.get().url)
      }
    }

    "Go to the 'check company' page" when {
      "the includeCompanyNotRegistered is true" in new Fixture {
        val model  = createModel(Some(BusinessAppliedForFrnYes("1234567")))
        val result = router.getRoute("internalId", model, includeCompanyNotRegistered = true)

        redirectLocation(result) mustBe Some(routes.CheckCompanyController.get().url)
      }
    }
  }
}
