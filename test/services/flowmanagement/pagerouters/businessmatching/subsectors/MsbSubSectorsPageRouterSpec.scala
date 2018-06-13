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

import models.businessmatching.{ChequeCashingNotScrapMetal, ChequeCashingScrapMetal, CurrencyExchange, TransmittingMoney}
import models.flowmanagement.ChangeSubSectorFlowModel
import org.scalatest.concurrent.ScalaFutures
import utils.AmlsSpec
import controllers.businessmatching.routes
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

class MsbSubSectorsPageRouterSpec extends AmlsSpec with ScalaFutures {

  trait Fixture {

    val router = new MsbSubSectorsPageRouter

  }

  "Getting the next route" must {
    "route to the 'Check your answers' page" when {
      Set(CurrencyExchange, ChequeCashingScrapMetal, ChequeCashingNotScrapMetal) foreach { s =>
        s"$s has been chosen" in new Fixture {
          val model = ChangeSubSectorFlowModel(Some(Set(s)))
          val result = router.getPageRoute(model)

          redirectLocation(result) mustBe Some(routes.SummaryController.get().url)
        }
      }
    }

    "route to the 'PSR number' page" when {
      "TransmittingMoney has been chosen" in new Fixture {
        val model = ChangeSubSectorFlowModel(Some(Set(TransmittingMoney)))
        val result = router.getPageRoute(model)

        redirectLocation(result) mustBe Some(routes.PSRNumberController.get().url)
      }
    }

    "return a InternalServerError" when {
      "there is no data in the flow model" in new Fixture {
        val result = router.getPageRoute(ChangeSubSectorFlowModel())

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
