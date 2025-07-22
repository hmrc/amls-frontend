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
import views.html.renewal.YourResponsibilitiesView

class YourResponsibilitiesViewSpec extends AmlsViewSpec with Matchers {

  lazy val your_responsibilities                                 = inject[YourResponsibilitiesView]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture

  "YourResponsibilitiesView" must {
    "Have the correct title" in new ViewFixture {
      def view = your_responsibilities(None, None)

      doc.title must startWith(messages("title.yr"))
    }

    "Have the correct Headings" in new ViewFixture {
      def view = your_responsibilities(None, None)

      heading.html    must be(messages("title.yr"))
      subHeading.html must include(messages("summary.renewal"))
    }

    "contain the expected content elements for any renewal" in new ViewFixture {
      def view = your_responsibilities(None, None)

      html must include(messages("renewal.yourresponsibilities.warning.text"))
      html must include(messages("renewal.yourresponsibilities.para2.start"))
      html must include(messages("renewal.yourresponsibilities.para2.link"))
      html must include(messages("renewal.yourresponsibilities.para3"))
    }

    "contain the expected content elements for AMP renewal" in new ViewFixture {
      def view = your_responsibilities(Some(BusinessActivities(Set(ArtMarketParticipant))), None)

      html must include(messages("renewal.yourresponsibilities.warning.text"))
      html must include(messages("renewal.yourresponsibilities.para2.start"))
      html must include(messages("renewal.yourresponsibilities.para2.link"))
      html must include(messages("renewal.yourresponsibilities.para3"))
    }

    "contain the expected content elements for MSB renewal" in new ViewFixture {
      def view = your_responsibilities(Some(BusinessActivities(Set(MoneyServiceBusiness))), None)

      html must include(messages("renewal.yourresponsibilities.warning.text"))
      html must include(messages("renewal.yourresponsibilities.para2.start"))
      html must include(messages("renewal.yourresponsibilities.para2.link"))
      html must include(messages("renewal.yourresponsibilities.para3"))
    }

    "contain the expected content elements for MSB renewal with MoneyTransmitting" in new ViewFixture {
      def view = your_responsibilities(
        Some(BusinessActivities(Set(MoneyServiceBusiness))),
        Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))
      )

      html must include(messages("renewal.yourresponsibilities.warning.text"))
      html must include(messages("renewal.yourresponsibilities.para2.start"))
      html must include(messages("renewal.yourresponsibilities.para2.link"))
      html must include(messages("renewal.yourresponsibilities.para3"))
    }

    "contain the expected content elements for MSB renewal with Currency Exchange" in new ViewFixture {
      def view = your_responsibilities(
        Some(BusinessActivities(Set(MoneyServiceBusiness))),
        Some(BusinessMatchingMsbServices(Set(CurrencyExchange)))
      )

      html must include(messages("renewal.yourresponsibilities.warning.text"))
      html must include(messages("renewal.yourresponsibilities.para2.start"))
      html must include(messages("renewal.yourresponsibilities.para2.link"))
      html must include(messages("renewal.yourresponsibilities.para3"))
    }

    "contain the expected content elements for MSB renewal with Foreign Exchange" in new ViewFixture {
      def view = your_responsibilities(
        Some(BusinessActivities(Set(MoneyServiceBusiness))),
        Some(BusinessMatchingMsbServices(Set(ForeignExchange)))
      )

      html must include(messages("renewal.yourresponsibilities.warning.text"))
      html must include(messages("renewal.yourresponsibilities.para2.start"))
      html must include(messages("renewal.yourresponsibilities.para2.link"))
      html must include(messages("renewal.yourresponsibilities.para3"))
    }

    "contain the expected content elements for ASP renewal" in new ViewFixture {
      def view = your_responsibilities(Some(BusinessActivities(Set(AccountancyServices))), None)

      html must include(messages("renewal.yourresponsibilities.warning.text"))
      html must include(messages("renewal.yourresponsibilities.para2.start"))
      html must include(messages("renewal.yourresponsibilities.para2.link"))
      html must include(messages("renewal.yourresponsibilities.para3"))
    }

    "contain the expected content elements for HVD renewal" in new ViewFixture {
      def view = your_responsibilities(Some(BusinessActivities(Set(HighValueDealing))), None)

      html must include(messages("renewal.yourresponsibilities.warning.text"))
      html must include(messages("renewal.yourresponsibilities.para2.start"))
      html must include(messages("renewal.yourresponsibilities.para2.link"))
      html must include(messages("renewal.yourresponsibilities.para3"))
    }

    behave like pageWithBackLink(your_responsibilities(None, None))
  }
}
