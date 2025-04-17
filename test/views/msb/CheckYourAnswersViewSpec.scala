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

package views.msb

import models.Country
import models.businessmatching.BusinessMatchingMsbService.{CurrencyExchange, ForeignExchange, TransmittingMoney}
import models.businessmatching.BusinessMatchingMsbServices
import models.moneyservicebusiness._
import org.jsoup.Jsoup
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsSummaryViewSpec
import utils.msb.CheckYourAnswersHelper
import views.Fixture
import views.html.msb.CheckYourAnswersView

import scala.jdk.CollectionConverters._

class CheckYourAnswersViewSpec extends AmlsSummaryViewSpec with TableDrivenPropertyChecks {

  trait ViewFixture extends Fixture {
    lazy val summary                                               = inject[CheckYourAnswersView]
    lazy val helper                                                = inject[CheckYourAnswersHelper]
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView(FakeRequest())

    val fullMSB: MoneyServiceBusiness = MoneyServiceBusiness(
      Some(ExpectedThroughput.First),
      Some(BusinessUseAnIPSPNo),
      Some(IdentifyLinkedTransactions(false)),
      Some(
        WhichCurrencies(
          Seq("USD", "GBP", "EUR"),
          Some(UsesForeignCurrenciesYes),
          Some(MoneySources(Some(BankMoneySource("Banks")), Some(WholesalerMoneySource("Wholesalers")), Some(true)))
        )
      ),
      Some(SendMoneyToOtherCountry(true)),
      Some(FundsTransfer(false)),
      Some(
        BranchesOrAgents(
          BranchesOrAgentsHasCountries(true),
          Some(BranchesOrAgentsWhichCountries(Seq(Country("United Kingdom", "GB"))))
        )
      ),
      Some(SendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB")))),
      Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
      Some(TransactionsInNext12Months("10")),
      Some(CETransactionsInNext12Months("10")),
      Some(FXTransactionsInNext12Months("13"))
    )
  }

  val bmMsbServices = BusinessMatchingMsbServices(BusinessMatchingMsbServices.all.toSet)

  "CheckYourAnswersView" must {
    "have correct title" in new ViewFixture {

      def view = summary(helper.getSummaryList(fullMSB, bmMsbServices))

      doc.title must be(
        messages("title.cya") +
          " - " + messages("summary.msb") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
    }

    "have correct headings" in new ViewFixture {

      def view = summary(helper.getSummaryList(fullMSB, bmMsbServices))

      heading.html    must be(messages("title.cya"))
      subHeading.html must include(messages("summary.msb"))
    }

    "include the provided data" in new ViewFixture {

      val list = helper.getSummaryList(
        fullMSB,
        BusinessMatchingMsbServices(BusinessMatchingMsbServices.all.toSet)
      )

      def view = summary(list)

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

    trait NoSubsectorsViewFixture extends ViewFixture {
      def view = summary(helper.getSummaryList(fullMSB, BusinessMatchingMsbServices(Set())))
    }

    trait TMViewFixture extends ViewFixture {
      def view = summary(helper.getSummaryList(fullMSB, BusinessMatchingMsbServices(Set(TransmittingMoney))))
    }

    trait TMNotSendViewFixture extends ViewFixture {
      def view = summary(
        helper.getSummaryList(
          fullMSB.copy(sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false))),
          BusinessMatchingMsbServices(Set(TransmittingMoney))
        )
      )
    }

    trait CEViewFixture extends ViewFixture {
      def view = summary(helper.getSummaryList(fullMSB, BusinessMatchingMsbServices(Set(CurrencyExchange))))
    }

    trait FXViewFixture extends ViewFixture {
      def view = summary(helper.getSummaryList(fullMSB, BusinessMatchingMsbServices(Set(ForeignExchange))))
    }

    "business use an IPSP" when {
      "should display when MSB services contains TransmittingMoney" in new TMViewFixture {
        html must include(messages("msb.ipsp.title"))
      }

      "should not display when MSB services does not contain TransmittingMoney" in new NoSubsectorsViewFixture {
        html must not include messages("msb.ipsp.title")
      }
    }

    "funds transfer" when {
      "should display when MSB services contains TransmittingMoney" in new TMViewFixture {
        html must include(messages("msb.fundstransfer.title"))
      }

      "should not display when MSB services does not contain TransmittingMoney" in new NoSubsectorsViewFixture {
        html must not include messages("msb.fundstransfer.title")
      }
    }

    "transactions in next 12 months" when {
      "should display when MSB services contains TransmittingMoney" in new TMViewFixture {
        html must include(messages("msb.transactions.expected.cya"))
      }

      "should not display when MSB services does not contain TransmittingMoney" in new NoSubsectorsViewFixture {
        html must not include messages("msb.transactions.expected.cya")
      }
    }

    "send money to other countries" when {
      "should display when MSB services contains TransmittingMoney" in new TMViewFixture {
        html must include(messages("msb.send.money.title"))
      }

      "should not display when MSB services does not contain TransmittingMoney" in new NoSubsectorsViewFixture {
        html must not include messages("msb.send.money.title")
      }
    }

    "send largest amount of money to which countries" when {
      "should display when MSB services contains TransmittingMoney and sends money to other countries" in new TMViewFixture {
        html must include(messages("msb.send.the.largest.amounts.of.money.cya"))
      }

      "should not display when MSB services contains TransmittingMoney and does not send money to other countries" in new TMNotSendViewFixture {
        html must not include messages("msb.send.the.largest.amounts.of.money.cya")
      }

      "should not display when MSB services does not contain TransmittingMoney" in new NoSubsectorsViewFixture {
        html must not include messages("msb.send.the.largest.amounts.of.money.cya")
      }
    }

    "largest amount of transactions in which countries" when {
      "should display when MSB services contains TransmittingMoney and sends money to other countries" in new TMViewFixture {
        html must include(messages("msb.most.transactions.cya"))
      }

      "should not display when MSB services contains TransmittingMoney and does not send money to other countries" in new TMNotSendViewFixture {
        html must not include messages("msb.most.transactions.cya")
      }

      "should not display when MSB services does not contain TransmittingMoney" in new NoSubsectorsViewFixture {
        html must not include messages("msb.most.transactions.cya")
      }
    }

    "CE transactions in next 12 months" when {
      "should display when MSB services contains CurrencyExchange" in new CEViewFixture {
        html must include(messages("msb.ce.transactions.expected.in.12.months.cya"))
      }

      "should not display when MSB services does not contain CurrencyExchange" in new NoSubsectorsViewFixture {
        html must not include messages("msb.ce.transactions.expected.in.12.months.cya")
      }
    }

    "which currencies to deal with" when {
      "should display when MSB services contains CurrencyExchange" in new CEViewFixture {
        html must include(messages("msb.which_currencies.cya"))
      }

      "should not display when MSB services does not contain CurrencyExchange" in new NoSubsectorsViewFixture {
        html must not include messages("msb.which_currencies.cya")
      }
    }

    "FX transactions in next 12 months" when {
      "should display when MSB services contains ForeignExchange" in new FXViewFixture {
        html must include(messages("msb.fx.transactions.expected.in.12.months.cya"))
      }

      "should not display when MSB services does not contain ForeignExchange" in new NoSubsectorsViewFixture {
        html must not include messages("msb.fx.transactions.expected.in.12.months.cya")
      }
    }

  }
}
