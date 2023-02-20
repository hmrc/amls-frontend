/*
 * Copyright 2023 HM Revenue & Customs
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

import models.businessmatching.{AccountancyServices, ArtMarketParticipant, BusinessActivities, BusinessMatchingMsbServices, CurrencyExchange, ForeignExchange, HighValueDealing, MoneyServiceBusiness, TransmittingMoney}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.what_you_need


class what_you_needSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val what_you_need = app.injector.instanceOf[what_you_need]
    implicit val requestWithToken = addTokenForView()
  }

  "What you need View" must {
    "Have the correct title" in new ViewFixture {
      def view = what_you_need(None, None)

      doc.title must startWith(Messages("title.wyn"))
    }

    "Have the correct Headings" in new ViewFixture{
      def view = what_you_need(None, None)

      heading.html must be (Messages("title.wyn"))
      subHeading.html must include (Messages("summary.renewal"))
    }

    "contain the expected content elements for any renewal" in new ViewFixture{
      def view = what_you_need(None, None)

      html must include(Messages("about any other activities your business was involved in over the last 12 months"))
      html must include(Messages("your total net profit for the last 12 months, if your business was involved in other activities"))
      html must include(Messages("your total net profit for the last 12 months from the services you have registered"))
    }

    "contain the expected content elements for AMP renewal" in new ViewFixture{
      def view = what_you_need(Some(BusinessActivities(Set(ArtMarketParticipant))), None)

      html must include(Messages("about any other activities your business was involved in over the last 12 months"))
      html must include(Messages("your total net profit for the last 12 months, if your business was involved in other activities"))
      html must include(Messages("your total net profit for the last 12 months from the services you have registered"))
      html must include(Messages("the percentage of your turnover that came from sales of art for €10,000 or more"))
    }

    "contain the expected content elements for MSB renewal" in new ViewFixture{
      def view = what_you_need(Some(BusinessActivities(Set(MoneyServiceBusiness))), None)

      html must include(Messages("about any other activities your business was involved in over the last 12 months"))
      html must include(Messages("your total net profit for the last 12 months, if your business was involved in other activities"))
      html must include(Messages("your total net profit for the last 12 months from the services you have registered"))
      html must include(Messages("your total value of transactions in the last 12 months"))
    }

    "contain the expected content elements for MSB renewal with MoneyTransmitting" in new ViewFixture{
      def view = what_you_need(Some(BusinessActivities(Set(MoneyServiceBusiness))), Some(BusinessMatchingMsbServices(Set(TransmittingMoney))))

      html must include(Messages("about any other activities your business was involved in over the last 12 months"))
      html must include(Messages("your total net profit for the last 12 months, if your business was involved in other activities"))
      html must include(Messages("your total net profit for the last 12 months from the services you have registered"))
      html must include(Messages("your total value of transactions in the last 12 months"))
      html must include(Messages("the number of money transfers you made in the last 12 months"))
      html must include(Messages("which countries you sent the largest amounts of money to, if you sent money to other countries in the last 12 months"))
      html must include(Messages("which countries you sent the most transactions to, if you sent money to other countries in the last 12 months"))
    }

    "contain the expected content elements for MSB renewal with Currency Exchange" in new ViewFixture{
      def view = what_you_need(Some(BusinessActivities(Set(MoneyServiceBusiness))), Some(BusinessMatchingMsbServices(Set(CurrencyExchange))))

      html must include(Messages("about any other activities your business was involved in over the last 12 months"))
      html must include(Messages("your total net profit for the last 12 months, if your business was involved in other activities"))
      html must include(Messages("your total net profit for the last 12 months from the services you have registered"))
      html must include(Messages("your total value of transactions in the last 12 months"))
      html must include(Messages("the number of currency exchange transactions you made in the last 12 months"))
      html must include(Messages("which currencies you supplied the most to your customers"))
      html must include(Messages("who supplied your foreign currency, if you dealt in physical foreign currencies"))
    }

    "contain the expected content elements for MSB renewal with Foreign Exchange" in new ViewFixture{
      def view = what_you_need(Some(BusinessActivities(Set(MoneyServiceBusiness))), Some(BusinessMatchingMsbServices(Set(ForeignExchange))))

      html must include(Messages("about any other activities your business was involved in over the last 12 months"))
      html must include(Messages("your total net profit for the last 12 months, if your business was involved in other activities"))
      html must include(Messages("your total net profit for the last 12 months from the services you have registered"))
      html must include(Messages("your total value of transactions in the last 12 months"))
      html must include(Messages("the number of foreign exchange transactions you made in the last 12 months"))
    }

    "contain the expected content elements for ASP renewal" in new ViewFixture{
      def view = what_you_need(Some(BusinessActivities(Set(AccountancyServices))), None)

      html must include(Messages("about any other activities your business was involved in over the last 12 months"))
      html must include(Messages("your total net profit for the last 12 months, if your business was involved in other activities"))
      html must include(Messages("your total net profit for the last 12 months from the services you have registered"))
      html must include(Messages("which countries your customers lived or worked in, if you had customers who lived or worked outside of the UK"))
    }

    "contain the expected content elements for HVD renewal" in new ViewFixture{
      def view = what_you_need(Some(BusinessActivities(Set(HighValueDealing))), None)

      html must include(Messages("about any other activities your business was involved in over the last 12 months"))
      html must include(Messages("your total net profit for the last 12 months, if your business was involved in other activities"))
      html must include(Messages("your total net profit for the last 12 months from the services you have registered"))
      html must include(Messages("which countries your customers lived or worked in, if you had customers who lived or worked outside of the UK"))
      html must include(Messages("the percentage of your turnover that came from cash payments of €10,000 or more"))
      html must include(Messages("how you received cash payments of €10,000 or more from customers you had not met in person, if you received any"))
    }

    "have a back link" in new ViewFixture {
      def view = what_you_need(None, None)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}
