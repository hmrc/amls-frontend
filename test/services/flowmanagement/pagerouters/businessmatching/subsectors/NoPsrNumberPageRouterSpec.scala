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
import models.flowmanagement.ChangeSubSectorFlowModel
import utils.AmlsSpec
import play.api.test.Helpers._

class NoPsrNumberPageRouterSpec extends AmlsSpec {

  trait Fixture {
    val router = new NoPsrNumberPageRouter
  }

  trait NotRegisteredFixture {
    val router = new NoPsrNumberPageRouterCompanyNotRegistered
  }

  "NoPsrNumberPageRouter" must {
    "route to the 'Check your answers' page" in new Fixture {
      val model  = ChangeSubSectorFlowModel()
      val result = router.getRoute("internalId", model)

      redirectLocation(result) mustBe Some(routes.SummaryController.get().url)
    }
  }

  "NoPsrNumberPageRouter" must {
    "route to the 'Check your answers' page if company not registered is false" in new NotRegisteredFixture {
      val model  = ChangeSubSectorFlowModel()
      val result = router.getRoute("internalId", model)

      redirectLocation(result) mustBe Some(routes.SummaryController.get().url)
    }
    "route to the 'Check company' page if company not registered is true" in new NotRegisteredFixture {
      val model  = ChangeSubSectorFlowModel()
      val result = router.getRoute("internalId", model, includeCompanyNotRegistered = true)

      redirectLocation(result) mustBe Some(routes.CheckCompanyController.get().url)
    }
  }
}
