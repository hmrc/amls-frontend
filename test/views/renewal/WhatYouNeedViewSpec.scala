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

package views.renewal

import models.businessmatching.{BusinessActivities, BusinessMatchingMsbServices}
import models.businessmatching.BusinessActivity.{AccountancyServices, ArtMarketParticipant, HighValueDealing, MoneyServiceBusiness}
import models.businessmatching.BusinessMatchingMsbService.{CurrencyExchange, ForeignExchange, TransmittingMoney}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.WhatYouNeedView

class WhatYouNeedViewSpec extends AmlsViewSpec with Matchers {

  lazy val what_you_need                                         = inject[WhatYouNeedView]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture

  "WhatYouNeedView" must {
    "Have the correct title" in new ViewFixture {
      def view = what_you_need(None, None)

      doc.title must startWith(messages("title.wyn"))
    }

    "Have the correct Headings" in new ViewFixture {
      def view = what_you_need(None, None)

      heading.html    must be(messages("title.wyn"))
      subHeading.html must include(messages("summary.renewal"))
    }

    "contain the expected content elements for any renewal" in new ViewFixture {
      def view = what_you_need(None, None)

      html must include(messages("about any other activities your business was involved in over the last 12 months"))
      html must include(
        messages("your total net profit for the last 12 months, if your business was involved in other activities")
      )
      html must include(messages("your total net profit for the last 12 months from the services you have registered"))
    }

    "contain the expected content elements for AMP renewal" in new ViewFixture {
      def view = what_you_need(Some(BusinessActivities(Set(ArtMarketParticipant))), None)

      html must include(messages("about any other activities your business was involved in over the last 12 months"))
      html must include(
        messages("your total net profit for the last 12 months, if your business was involved in other activities")
      )
      html must include(messages("your total net profit for the last 12 months from the services you have registered"))
      html must include(messages("the percentage of your turnover that came from sales of art for €10,000 or more"))
    }

    "contain the expected content elements for MSB renewal" in new ViewFixture {
      def view = what_you_need(Some(BusinessActivities(Set(MoneyServiceBusiness))), None)

      html must include(messages("about any other activities your business was involved in over the last 12 months"))
      html must include(
        messages("your total net profit for the last 12 months, if your business was involved in other activities")
      )
      html must include(messages("your total net profit for the last 12 months from the services you have registered"))
      html must include(messages("your total value of transactions in the last 12 months"))
    }

    "contain the expected content elements for MSB renewal with MoneyTransmitting" in new ViewFixture {
      def view = what_you_need(
        Some(BusinessActivities(Set(MoneyServiceBusiness))),
        Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))
      )

      html must include(messages("about any other activities your business was involved in over the last 12 months"))
      html must include(
        messages("your total net profit for the last 12 months, if your business was involved in other activities")
      )
      html must include(messages("your total net profit for the last 12 months from the services you have registered"))
      html must include(messages("your total value of transactions in the last 12 months"))
      html must include(messages("the number of money transfers you made in the last 12 months"))
      html must include(
        messages(
          "which countries you sent the largest amounts of money to, if you sent money to other countries in the last 12 months"
        )
      )
      html must include(
        messages(
          "which countries you sent the most transactions to, if you sent money to other countries in the last 12 months"
        )
      )
    }

    "contain the expected content elements for MSB renewal with Currency Exchange" in new ViewFixture {
      def view = what_you_need(
        Some(BusinessActivities(Set(MoneyServiceBusiness))),
        Some(BusinessMatchingMsbServices(Set(CurrencyExchange)))
      )

      html must include(messages("about any other activities your business was involved in over the last 12 months"))
      html must include(
        messages("your total net profit for the last 12 months, if your business was involved in other activities")
      )
      html must include(messages("your total net profit for the last 12 months from the services you have registered"))
      html must include(messages("your total value of transactions in the last 12 months"))
      html must include(messages("the number of currency exchange transactions you made in the last 12 months"))
      html must include(messages("which currencies you supplied the most to your customers"))
      html must include(messages("who supplied your foreign currency, if you dealt in physical foreign currencies"))
    }

    "contain the expected content elements for MSB renewal with Foreign Exchange" in new ViewFixture {
      def view = what_you_need(
        Some(BusinessActivities(Set(MoneyServiceBusiness))),
        Some(BusinessMatchingMsbServices(Set(ForeignExchange)))
      )

      html must include(messages("about any other activities your business was involved in over the last 12 months"))
      html must include(
        messages("your total net profit for the last 12 months, if your business was involved in other activities")
      )
      html must include(messages("your total net profit for the last 12 months from the services you have registered"))
      html must include(messages("your total value of transactions in the last 12 months"))
      html must include(messages("the number of foreign exchange transactions you made in the last 12 months"))
    }

    "contain the expected content elements for ASP renewal" in new ViewFixture {
      def view = what_you_need(Some(BusinessActivities(Set(AccountancyServices))), None)

      html must include(messages("about any other activities your business was involved in over the last 12 months"))
      html must include(
        messages("your total net profit for the last 12 months, if your business was involved in other activities")
      )
      html must include(messages("your total net profit for the last 12 months from the services you have registered"))
      html must include(
        messages(
          "which countries your customers lived or worked in, if you had customers who lived or worked outside of the UK"
        )
      )
    }

    "contain the expected content elements for HVD renewal" in new ViewFixture {
      def view = what_you_need(Some(BusinessActivities(Set(HighValueDealing))), None)

      html must include(messages("about any other activities your business was involved in over the last 12 months"))
      html must include(
        messages("your total net profit for the last 12 months, if your business was involved in other activities")
      )
      html must include(messages("your total net profit for the last 12 months from the services you have registered"))
      html must include(
        messages(
          "which countries your customers lived or worked in, if you had customers who lived or worked outside of the UK"
        )
      )
      html must include(messages("the percentage of your turnover that came from cash payments of €10,000 or more"))
      html must include(
        messages(
          "how you received cash payments of €10,000 or more from customers you had not met in person, if you received any"
        )
      )
    }

    behave like pageWithBackLink(what_you_need(None, None))
  }
}
