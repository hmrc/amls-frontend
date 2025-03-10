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

package services.msb

import models.businessmatching.BusinessMatchingMsbService.{ForeignExchange, TransmittingMoney}
import models.businessmatching.{BusinessMatching, BusinessMatchingMsbServices}
import models.moneyservicebusiness.{FXTransactionsInNext12Months, MoneyServiceBusiness}
import play.api.mvc.Results
import utils.AmlsSpec

class MoneySourcesServiceSpec extends AmlsSpec with Results {

  val service = new MoneySourcesService()

  "MoneySourcesService" must {

    "redirect to the Foreign Exchange In Next 12 Months page" when {

      "MSB Services contains Foreign Exchange and user answers for FX-Transactions is empty" in {

        val msb = Some(MoneyServiceBusiness())
        val bm  = Some(BusinessMatching(msbServices = Some(BusinessMatchingMsbServices(Set(ForeignExchange)))))

        service.redirectToNextPage(msb, bm, edit = true) mustBe Some(
          Redirect(controllers.msb.routes.FXTransactionsInNext12MonthsController.get(true))
        )
      }

      "MSB Services contains Foreign Exchange and edit is false" in {

        val msb = Some(MoneyServiceBusiness(fxTransactionsInNext12Months = Some(FXTransactionsInNext12Months("foo"))))
        val bm  = Some(BusinessMatching(msbServices = Some(BusinessMatchingMsbServices(Set(ForeignExchange)))))

        service.redirectToNextPage(msb, bm, edit = false) mustBe Some(
          Redirect(controllers.msb.routes.FXTransactionsInNext12MonthsController.get(false))
        )
      }
    }

    "redirect to MSB Summary Page" when {

      "MoneyServiceBusiness contains Foreign Exchange and user answers for FX-Transactions is empty" in {

        val msb = Some(MoneyServiceBusiness(fxTransactionsInNext12Months = Some(FXTransactionsInNext12Months("foo"))))
        val bm  = Some(BusinessMatching(msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))))

        service.redirectToNextPage(msb, bm, edit = true) mustBe Some(
          Redirect(controllers.msb.routes.SummaryController.get)
        )
      }

      "MSB Services does not contain Foreign Exchange" in {

        val msb = Some(MoneyServiceBusiness())
        val bm  = Some(BusinessMatching(msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))))

        service.redirectToNextPage(msb, bm, edit = true) mustBe Some(
          Redirect(controllers.msb.routes.SummaryController.get)
        )
      }
    }
  }
}
