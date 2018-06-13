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

package services.flowmanagement.pagerouters.businessmatching.subsectors

import controllers.businessmatching.routes
import models.businessmatching.{BusinessAppliedForPSRNumber, BusinessAppliedForPSRNumberNo, BusinessAppliedForPSRNumberYes, TransmittingMoney}
import models.flowmanagement.ChangeSubSectorFlowModel
import utils.AmlsSpec

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.test.Helpers._

class PSRNumberPageRouterSpec extends AmlsSpec {

  trait Fixture {
    val router = new PSRNumberPageRouter

    val createModel: Option[BusinessAppliedForPSRNumber] => ChangeSubSectorFlowModel =
      ChangeSubSectorFlowModel.apply(Some(Set(TransmittingMoney)), _)
  }

  "Getting the next route" must {
    "redirect to the 'check your answers' page" when {
      "the user has entered a PSR number" in new Fixture {
        val model = createModel(Some(BusinessAppliedForPSRNumberYes("123456789")))
        val result = router.getPageRoute(model)

        redirectLocation(result) mustBe Some(routes.SummaryController.get().url)
      }
    }

    "route to the 'you can't continue with your change' page" when {
      "there is no PSR number" in new Fixture {
        val model = createModel(Some(BusinessAppliedForPSRNumberNo))
        val result = router.getPageRoute(model)

        redirectLocation(result) mustBe Some(routes.NoPsrController.get().url)
      }
    }

    "return an Internal Server Error" when {
      "there is no PSR number data" in new Fixture {
        val model = createModel(None)
        val result = router.getPageRoute(model)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

}
