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

import models.Country
import models.businessmatching._
import models.renewal._
import org.jsoup.Jsoup
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utils.AmlsSummaryViewSpec
import utils.renewal.CheckYourAnswersHelper
import views.Fixture
import views.html.renewal.CheckYourAnswersView

import scala.jdk.CollectionConverters._

class CheckYourAnswersViewSpec extends AmlsSummaryViewSpec with TableDrivenPropertyChecks {

  lazy val cyaView                                          = inject[CheckYourAnswersView]
  lazy val cyaHelper                                        = inject[CheckYourAnswersHelper]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  trait ViewFixture extends Fixture

  "CheckYourAnswersView" must {

    "have correct title" in new ViewFixture {
      def view = cyaView(cyaHelper.getSummaryList(Renewal(), None))

      doc.title must startWith(messages("title.cya") + " - " + messages("summary.renewal"))
    }

    "have correct headings" in new ViewFixture {
      def view = cyaView(cyaHelper.getSummaryList(Renewal(), None))

      heading.html    must be(messages("title.cya"))
      subHeading.html must include(messages("summary.renewal"))
    }

    "include the provided data" in new ViewFixture {

      val countries: Seq[Country] = Seq(
        Country("United Kingdom", "GB"),
        Country("United States", "US"),
        Country("Germany", "DE")
      )

      val model: Renewal = Renewal(
        Some(InvolvedInOtherYes("A description of activities")),
        Some(BusinessTurnover.First),
        Some(AMLSTurnover.First),
        Some(AMPTurnover.First),
        Some(CustomersOutsideIsUK(true)),
        Some(CustomersOutsideUK(Some(countries))),
        Some(PercentageOfCashPaymentOver15000.First),
        Some(
          CashPayments(
            CashPaymentsCustomerNotMet(true),
            Some(HowCashPaymentsReceived(PaymentMethods(true, true, Some("Third party"))))
          )
        ),
        Some(TotalThroughput("01")),
        Some(
          WhichCurrencies(
            Seq("GBP", "USD", "JPY"),
            Some(UsesForeignCurrenciesYes),
            Some(
              MoneySources(
                Some(BankMoneySource("Bank Name")),
                Some(WholesalerMoneySource("Wholesaler Name")),
                Some(true)
              )
            )
          )
        ),
        Some(TransactionsInLast12Months("1500")),
        Some(SendTheLargestAmountsOfMoney(countries)),
        Some(MostTransactions(countries)),
        Some(CETransactionsInLast12Months("123")),
        Some(FXTransactionsInLast12Months("12")),
        false,
        Some(SendMoneyToOtherCountry(true)),
        hasAccepted = true
      )

      val businessMatching = BusinessMatching(
        activities = Some(BusinessActivities(BusinessActivities.all)),
        msbServices = Some(BusinessMatchingMsbServices(BusinessMatchingMsbServices.all.toSet))
      )

      val list = cyaHelper.getSummaryList(model, businessMatching)

      def view = cyaView(list)

      doc
        .getElementsByClass("govuk-summary-list__key")
        .asScala
        .zip(
          doc.getElementsByClass("govuk-summary-list__value").asScala
        )
        .foreach { case (key, value) =>
          val maybeRow = list.rows.find(_.key.content.asHtml.body == key.text()).value

          maybeRow.key.content.asHtml.body must include(key.text())

          val valueText = maybeRow.value.content.asHtml.body match {
            case str if str.startsWith("<") => Jsoup.parse(str).text()
            case str                        => str
          }

          valueText must include(value.text())
        }
    }
  }
}
