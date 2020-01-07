/*
 * Copyright 2020 HM Revenue & Customs
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

package views.msb

import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching.{BusinessMatchingMsbServices, CurrencyExchange}
import models.moneyservicebusiness.{MoneyServiceBusiness, MoneySources, UsesForeignCurrenciesYes, WhichCurrencies}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture


class summaryPreRelease7Spec extends  AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "MSB Summary page" should {

    "indicate whether foreign currencies are used" in new ViewFixture {

      val model = MoneyServiceBusiness(whichCurrencies = Some(WhichCurrencies(Seq("GBP"), None, None)))

      def view = views.html.msb.summary(model, Some(BusinessMatchingMsbServices(Set(CurrencyExchange))), ServiceChangeRegister())

      html.contains(Messages("msb.which_currencies.foreign_currencies_question")) must be(false)

    }

  }

}