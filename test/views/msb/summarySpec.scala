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

package views.msb

import models.Country
import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching.{BusinessMatchingMsbServices, CurrencyExchange, ForeignExchange, TransmittingMoney}
import models.moneyservicebusiness._
import org.jsoup.nodes.Element
import org.scalatest.MustMatchers
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.i18n.Messages
import utils.AmlsSpec
import views.{Fixture, HtmlAssertions}

import scala.collection.JavaConversions._

class summarySpec extends AmlsSpec
  with MustMatchers
  with HtmlAssertions
  with TableDrivenPropertyChecks {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "summary view" must {
    "have correct title" in new ViewFixture {

      def view = views.html.msb.summary(MoneyServiceBusiness(), None, true, ServiceChangeRegister())

      doc.title must be(Messages("title.cya") +
        " - " + Messages("summary.msb") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      def view = views.html.msb.summary(MoneyServiceBusiness(), None, true, ServiceChangeRegister())

      heading.html must be(Messages("title.cya"))
      subHeading.html must include(Messages("summary.msb"))
    }

    val sectionChecks = Table[String, Element => Boolean](
      ("title key", "check"),
      ("msb.throughput.title",checkElementTextIncludes(_, "msb.throughput.lbl.01")),
      ("msb.ipsp.title",checkElementTextIncludes(_, "lbl.no")),
      ("msb.linked.txn.title",checkElementTextIncludes(_, "lbl.no")),
      ("msb.which_currencies.title",checkElementTextIncludes(_, "USD","GBP","EUR","bank names","Wholesaler Names")),
      ("msb.send.money.title",checkElementTextIncludes(_, "lbl.yes")),
      ("msb.fundstransfer.title",checkElementTextIncludes(_, "lbl.no")),
      ("msb.branchesoragents.title",checkElementTextIncludes(_, "United Kingdom")),
      ("msb.send.the.largest.amounts.of.money.title",checkElementTextIncludes(_, "United Kingdom")),
      ("msb.most.transactions.title",checkElementTextIncludes(_, "United Kingdom")),
      ("msb.transactions.expected.title",checkElementTextIncludes(_, "10")),
      ("msb.ce.transactions.expected.in.12.months.title",checkElementTextIncludes(_, "10")),
      ("msb.fx.transactions.expected.in.12.months.title",checkElementTextIncludes(_, "13"))
    )

    val fullMSB: MoneyServiceBusiness = MoneyServiceBusiness(
      Some(ExpectedThroughput.First),
      Some(BusinessUseAnIPSPNo),
      Some(IdentifyLinkedTransactions(false)),
      Some(WhichCurrencies(Seq("USD", "GBP", "EUR"),
        usesForeignCurrencies = Some(true),
        Some(BankMoneySource("bank names")),
        Some(WholesalerMoneySource("Wholesaler Names")),
        Some(true))),
      Some(SendMoneyToOtherCountry(true)),
      Some(FundsTransfer(false)),
      Some(BranchesOrAgents(Some(Seq(Country("United Kingdom", "GB"))))),
      Some(SendTheLargestAmountsOfMoney(Country("United Kingdom", "GB"))),
      Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
      Some(TransactionsInNext12Months("10")),
      Some(CETransactionsInNext12Months("10")),
      Some(FXTransactionsInNext12Months("13"))
    )

    val msbServices: BusinessMatchingMsbServices = BusinessMatchingMsbServices(Set(CurrencyExchange, TransmittingMoney, ForeignExchange))

    "include the provided data" in new ViewFixture {

      def view = views.html.msb.summary(
        fullMSB,
        Some(msbServices),
        true,
        ServiceChangeRegister()
      )

      forAll(sectionChecks) {
        (key, check) => {
          val hTwos = doc.select("section.check-your-answers h2")
          val hTwo = hTwos.toList.find(e => e.text() == Messages(key))

          hTwo must not be None
          val section = hTwo.get.parents().select("section").first()
          check(section) must be(true)
        }
      }
    }

    trait NoSubsectorsViewFixture extends ViewFixture {
      def view = views.html.msb.summary(fullMSB, Some(BusinessMatchingMsbServices(Set())), true, ServiceChangeRegister())
    }

    trait TMViewFixture extends ViewFixture {
      def view = views.html.msb.summary(fullMSB, Some(BusinessMatchingMsbServices(Set(TransmittingMoney))), true, ServiceChangeRegister())
    }

    trait TMNotSendViewFixture extends ViewFixture {
      def view = views.html.msb.summary(
        fullMSB.copy(sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false))),
        Some(BusinessMatchingMsbServices(Set(TransmittingMoney))), true, ServiceChangeRegister()
      )
    }

    trait CEViewFixture extends ViewFixture {
      def view = views.html.msb.summary(fullMSB, Some(BusinessMatchingMsbServices(Set(CurrencyExchange))), true, ServiceChangeRegister())
    }

    trait FXViewFixture extends ViewFixture {
      def view = views.html.msb.summary(fullMSB, Some(BusinessMatchingMsbServices(Set(ForeignExchange))), true, ServiceChangeRegister())
    }

    "business use an IPSP" when {
      "should display when MSB services contains TransmittingMoney" in new TMViewFixture {
        html must include(Messages("msb.ipsp.title"))
      }

      "should not display when MSB services does not contain TransmittingMoney" in new NoSubsectorsViewFixture {
        html must not include Messages("msb.ipsp.title")
      }
    }

    "funds transfer" when {
      "should display when MSB services contains TransmittingMoney" in new TMViewFixture {
        html must include(Messages("msb.fundstransfer.title"))
      }

      "should not display when MSB services does not contain TransmittingMoney" in new NoSubsectorsViewFixture {
        html must not include Messages("msb.fundstransfer.title")
      }
    }

    "transactions in next 12 months" when {
      "should display when MSB services contains TransmittingMoney" in new TMViewFixture {
        html must include(Messages("msb.transactions.expected.title"))
      }

      "should not display when MSB services does not contain TransmittingMoney" in new NoSubsectorsViewFixture {
        html must not include Messages("msb.transactions.expected.title")
      }
    }

    "send money to other countries" when {
      "should display when MSB services contains TransmittingMoney" in new TMViewFixture {
        html must include(Messages("msb.send.money.title"))
      }

      "should not display when MSB services does not contain TransmittingMoney" in new NoSubsectorsViewFixture {
        html must not include Messages("msb.send.money.title")
      }
    }

    "send largest amount of money to which countries" when {
      "should display when MSB services contains TransmittingMoney and sends money to other countries" in new TMViewFixture {
        html must include(Messages("msb.send.the.largest.amounts.of.money.title"))
      }

      "should not display when MSB services contains TransmittingMoney and does not send money to other countries" in new TMNotSendViewFixture {
        html must not include Messages("msb.send.the.largest.amounts.of.money.title")
      }

      "should not display when MSB services does not contain TransmittingMoney" in new NoSubsectorsViewFixture {
        html must not include Messages("msb.send.the.largest.amounts.of.money.title")
      }
    }

    "largest amount of transactions in which countries" when {
      "should display when MSB services contains TransmittingMoney and sends money to other countries" in new TMViewFixture {
        html must include(Messages("msb.most.transactions.title"))
      }

      "should not display when MSB services contains TransmittingMoney and does not send money to other countries" in new TMNotSendViewFixture {
        html must not include Messages("msb.most.transactions.title")
      }

      "should not display when MSB services does not contain TransmittingMoney" in new NoSubsectorsViewFixture {
        html must not include Messages("msb.most.transactions.title")
      }
    }

    "CE transactions in next 12 months" when {
      "should display when MSB services contains CurrencyExchange" in new CEViewFixture {
        html must include(Messages("msb.ce.transactions.expected.in.12.months.title"))
      }

      "should not display when MSB services does not contain CurrencyExchange" in new NoSubsectorsViewFixture {
        html must not include Messages("msb.ce.transactions.expected.in.12.months.title")
      }
    }

    "which currencies to deal with" when {
      "should display when MSB services contains CurrencyExchange" in new CEViewFixture {
        html must include(Messages("msb.which_currencies.title"))
      }

      "should not display when MSB services does not contain CurrencyExchange" in new NoSubsectorsViewFixture {
        html must not include Messages("msb.which_currencies.title")
      }
    }

    "FX transactions in next 12 months" when {
      "should display when MSB services contains ForeignExchange" in new FXViewFixture {
        html must include(Messages("msb.fx.transactions.expected.in.12.months.title"))
      }

      "should not display when MSB services does not contain ForeignExchange" in new NoSubsectorsViewFixture {
        html must not include Messages("msb.fx.transactions.expected.in.12.months.title")
      }
    }

  }
}