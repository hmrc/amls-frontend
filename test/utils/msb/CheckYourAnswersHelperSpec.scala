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

package utils.msb

import models.businessmatching.BusinessMatchingMsbService.{CurrencyExchange, ForeignExchange, TransmittingMoney}
import models.Country
import models.businessmatching.BusinessMatchingMsbServices
import models.moneyservicebusiness._
import org.scalatest.Assertion
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryListRow
import utils.AmlsSpec

class CheckYourAnswersHelperSpec extends AmlsSpec {

  lazy val cyaHelper: CheckYourAnswersHelper = app.injector.instanceOf[CheckYourAnswersHelper]

  val countries: Seq[Country] = Seq(
    Country("United Kingdom", "GB"),
    Country("United States", "US"),
    Country("Germany", "DE")
  )

  val currencies: Seq[String] = Seq("GBP", "USD", "JPY")

  val ipspName      = "John Smith"
  val ipspRefNumber = "ASDFGH123456789"

  val transactions   = "123"
  val ceTransactions = "456"
  val fxTransactions = "789"

  val bankName       = "A Big Bank"
  val wholesalerName = "Some Large Wholesaler"

  val fullMoneySourcesModel: MoneySources = MoneySources(
    Some(BankMoneySource(bankName)),
    Some(WholesalerMoneySource(wholesalerName)),
    Some(true)
  )

  val model: MoneyServiceBusiness = MoneyServiceBusiness(
    throughput = Some(ExpectedThroughput.Second),
    businessUseAnIPSP = Some(BusinessUseAnIPSPYes(ipspName, ipspRefNumber)),
    identifyLinkedTransactions = Some(IdentifyLinkedTransactions(true)),
    whichCurrencies = Some(
      WhichCurrencies(
        currencies,
        Some(UsesForeignCurrenciesYes),
        Some(fullMoneySourcesModel)
      )
    ),
    sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(true)),
    fundsTransfer = Some(FundsTransfer(true)),
    branchesOrAgents = Some(
      BranchesOrAgents(
        BranchesOrAgentsHasCountries(true),
        Some(BranchesOrAgentsWhichCountries(countries))
      )
    ),
    sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(countries)),
    mostTransactions = Some(MostTransactions(countries)),
    transactionsInNext12Months = Some(TransactionsInNext12Months(transactions)),
    ceTransactionsInNext12Months = Some(CETransactionsInNext12Months(ceTransactions)),
    fxTransactionsInNext12Months = Some(FXTransactionsInNext12Months(fxTransactions))
  )

  val bmMsbServices: BusinessMatchingMsbServices = BusinessMatchingMsbServices(BusinessMatchingMsbServices.all.toSet)

  trait RowFixture {

    val summaryListRows: Seq[SummaryListRow]

    def assertRowMatches(index: Int, title: String, value: String, changeUrl: String, changeId: String): Assertion = {

      val result = summaryListRows.lift(index).getOrElse(fail(s"Row for index $index does not exist"))

      result.key.toString must include(messages(title))

      result.value.toString must include(value)

      checkChangeLink(result, changeUrl, changeId)
    }

    def assertRowIsNotPresent(title: String): Assertion =
      summaryListRows.exists(_.key.content.asHtml.body == title) mustBe false

    def checkChangeLink(slr: SummaryListRow, href: String, id: String): Assertion = {
      val changeLink = slr.actions.flatMap(_.items.headOption).getOrElse(fail("No edit link present"))

      changeLink.content.toString must include(messages("button.edit"))
      changeLink.href mustBe href
      changeLink.attributes("id") mustBe id
    }

    def toBulletList[A](coll: Seq[A]): String =
      "<ul class=\"govuk-list govuk-list--bullet\">" +
        coll.map { x =>
          s"<li>$x</li>"
        }.mkString +
        "</ul>"

    def booleanToLabel(bool: Boolean)(implicit messages: Messages): String = if (bool) {
      messages("lbl.yes")
    } else {
      messages("lbl.no")
    }
  }

  "CheckYourAnswersHelper" must {

    "render the correct summary list row" that {

      "for the Expected Throughput row" must {

        ExpectedThroughput.all foreach { throughput =>
          s"display for ${throughput.toString} radio" in new RowFixture {
            override val summaryListRows: Seq[SummaryListRow] = cyaHelper
              .getSummaryList(
                model.copy(throughput = Some(throughput)),
                bmMsbServices
              )
              .rows

            assertRowMatches(
              0,
              "msb.throughput.cya",
              messages(s"msb.throughput.lbl.${throughput.value}"),
              controllers.msb.routes.ExpectedThroughputController.get(true).url,
              "msbthroughput-edit"
            )
          }
        }
      }

      "for the Branches and Agents in other countries row" must {

        "display correctly for 'Yes'" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model, bmMsbServices).rows

          assertRowMatches(
            1,
            "msb.branchesoragents.title",
            booleanToLabel(true),
            controllers.msb.routes.BranchesOrAgentsController.get(true).url,
            "msbbranchesoragents-edit"
          )
        }

        "display correctly for 'No'" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model.copy(branchesOrAgents = Some(BranchesOrAgents(BranchesOrAgentsHasCountries(false), None))),
              bmMsbServices
            )
            .rows

          assertRowMatches(
            1,
            "msb.branchesoragents.title",
            booleanToLabel(false),
            controllers.msb.routes.BranchesOrAgentsController.get(true).url,
            "msbbranchesoragents-edit"
          )
        }

        "for the Branches and Agents list countries row" must {

          "display correctly for multiple countries" in new RowFixture {
            override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model, bmMsbServices).rows

            assertRowMatches(
              2,
              "msb.branchesoragents.countries.cya",
              toBulletList(countries.map(_.name)),
              controllers.msb.routes.BranchesOrAgentsWhichCountriesController.get(true).url,
              "msbwhichcountries-edit"
            )
          }

          "display correctly for a single country" in new RowFixture {
            override val summaryListRows: Seq[SummaryListRow] = cyaHelper
              .getSummaryList(
                model.copy(branchesOrAgents =
                  Some(
                    BranchesOrAgents(
                      BranchesOrAgentsHasCountries(true),
                      Some(BranchesOrAgentsWhichCountries(Seq(countries.head)))
                    )
                  )
                ),
                bmMsbServices
              )
              .rows

            assertRowMatches(
              2,
              "msb.branchesoragents.countries.cya",
              countries.head.name,
              controllers.msb.routes.BranchesOrAgentsWhichCountriesController.get(true).url,
              "msbwhichcountries-edit"
            )
          }
        }
      }

      "for Identify Linked Transactions row" must {

        "display correctly for 'Yes'" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model, bmMsbServices).rows

          assertRowMatches(
            3,
            "msb.linked.txn.title",
            booleanToLabel(true),
            controllers.msb.routes.IdentifyLinkedTransactionsController.get(true).url,
            "msblinkedtransactions-edit"
          )
        }

        "display correctly for 'No'" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model.copy(identifyLinkedTransactions = Some(IdentifyLinkedTransactions(false))),
              bmMsbServices
            )
            .rows

          assertRowMatches(
            3,
            "msb.linked.txn.title",
            booleanToLabel(false),
            controllers.msb.routes.IdentifyLinkedTransactionsController.get(true).url,
            "msblinkedtransactions-edit"
          )
        }
      }

      "for Business Use an IPSP row" must {

        "display correctly for 'Yes'" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model, bmMsbServices).rows

          assertRowMatches(
            4,
            "msb.ipsp.title",
            booleanToLabel(true),
            controllers.msb.routes.BusinessUseAnIPSPController.get(true).url,
            "msbipsp-edit"
          )
        }

        "display correctly for 'No'" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model.copy(businessUseAnIPSP = Some(BusinessUseAnIPSPNo)),
              bmMsbServices
            )
            .rows

          assertRowMatches(
            4,
            "msb.ipsp.title",
            booleanToLabel(false),
            controllers.msb.routes.BusinessUseAnIPSPController.get(true).url,
            "msbipsp-edit"
          )
        }
      }

      "for Business Use an IPSP name row" must {

        "display correctly for name" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model, bmMsbServices).rows

          assertRowMatches(
            5,
            "msb.summary.ipsp-name",
            ipspName,
            controllers.msb.routes.BusinessUseAnIPSPController.get(true).url,
            "msbipsp-name-edit"
          )
        }
      }

      "for Business Use an IPSP reference number row" must {

        "display correctly for reference number" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model, bmMsbServices).rows

          assertRowMatches(
            6,
            "msb.summary.ipsp-registration-number",
            ipspRefNumber,
            controllers.msb.routes.BusinessUseAnIPSPController.get(true).url,
            "msbipsp-number-edit"
          )
        }
      }

      "for Funds Transfer row" must {

        "display correctly for 'Yes'" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model, bmMsbServices).rows

          assertRowMatches(
            7,
            "msb.fundstransfer.title",
            booleanToLabel(true),
            controllers.msb.routes.FundsTransferController.get(true).url,
            "msbfundstransfer-edit"
          )
        }

        "display correctly for 'No'" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model.copy(fundsTransfer = Some(FundsTransfer(false))),
              bmMsbServices
            )
            .rows

          assertRowMatches(
            7,
            "msb.fundstransfer.title",
            booleanToLabel(false),
            controllers.msb.routes.FundsTransferController.get(true).url,
            "msbfundstransfer-edit"
          )
        }
      }

      "for Transactions In Next 12 Months row" must {

        "display correctly" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model, bmMsbServices).rows

          assertRowMatches(
            8,
            "msb.transactions.expected.cya",
            transactions,
            controllers.msb.routes.TransactionsInNext12MonthsController.get(true).url,
            "msbtransactionsexpected-edit"
          )
        }
      }

      "for Send Money To Other Country row" must {

        "display correctly for 'Yes'" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model, bmMsbServices).rows

          assertRowMatches(
            9,
            "msb.send.money.title",
            booleanToLabel(true),
            controllers.msb.routes.SendMoneyToOtherCountryController.get(true).url,
            "msbsendmoneytoothercountries-edit"
          )
        }

        "display correctly for 'No'" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model.copy(sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false))),
              bmMsbServices
            )
            .rows

          assertRowMatches(
            9,
            "msb.send.money.title",
            booleanToLabel(false),
            controllers.msb.routes.SendMoneyToOtherCountryController.get(true).url,
            "msbsendmoneytoothercountries-edit"
          )
        }
      }

      "for Send The Largest Amounts Of Money row" must {

        "display correctly for multiple countries" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model, bmMsbServices).rows

          assertRowMatches(
            10,
            "msb.send.the.largest.amounts.of.money.cya",
            toBulletList(countries.map(_.name)),
            controllers.msb.routes.SendTheLargestAmountsOfMoneyController.get(true).url,
            "msbsendlargestamounts-edit"
          )
        }

        "display correctly for a single country" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model.copy(sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(Seq(countries.head)))),
              bmMsbServices
            )
            .rows

          assertRowMatches(
            10,
            "msb.send.the.largest.amounts.of.money.cya",
            countries.head.name,
            controllers.msb.routes.SendTheLargestAmountsOfMoneyController.get(true).url,
            "msbsendlargestamounts-edit"
          )
        }
      }

      "for Most Transactions row" must {

        "display correctly for multiple countries" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model, bmMsbServices).rows

          assertRowMatches(
            11,
            "msb.most.transactions.cya",
            toBulletList(countries.map(_.name)),
            controllers.msb.routes.MostTransactionsController.get(true).url,
            "msbmosttransactions-edit"
          )
        }

        "display correctly for a single country" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model.copy(mostTransactions = Some(MostTransactions(Seq(countries.head)))),
              bmMsbServices
            )
            .rows

          assertRowMatches(
            11,
            "msb.most.transactions.cya",
            countries.head.name,
            controllers.msb.routes.MostTransactionsController.get(true).url,
            "msbmosttransactions-edit"
          )
        }
      }

      "for Currency Exchange Transactions In Next 12 Months row" must {

        "display correctly" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model, bmMsbServices).rows

          assertRowMatches(
            12,
            "msb.ce.transactions.expected.in.12.months.cya",
            ceTransactions,
            controllers.msb.routes.CurrencyExchangesInNext12MonthsController.get(true).url,
            "msbcetransactionsexpected-edit"
          )
        }
      }

      "for Which Currencies row" must {

        "display correctly for multiple currencies" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model, bmMsbServices).rows

          assertRowMatches(
            13,
            "msb.which_currencies.cya",
            toBulletList(currencies),
            controllers.msb.routes.WhichCurrenciesController.get(true).url,
            "whichCurrencies-edit"
          )
        }

        "display correctly for a single currency" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model.copy(whichCurrencies = Some(WhichCurrencies(Seq(currencies.head)))),
              bmMsbServices
            )
            .rows

          assertRowMatches(
            13,
            "msb.which_currencies.cya",
            currencies.head,
            controllers.msb.routes.WhichCurrenciesController.get(true).url,
            "whichCurrencies-edit"
          )
        }
      }

      "for Uses Foreign Currencies row" must {

        "display correctly for 'Yes'" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model, bmMsbServices).rows

          assertRowMatches(
            14,
            "msb.deal_foreign_currencies.title",
            booleanToLabel(true),
            controllers.msb.routes.UsesForeignCurrenciesController.get(true).url,
            "usesForeignCurrencies-edit"
          )
        }

        "display correctly for 'No'" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model.copy(whichCurrencies = Some(WhichCurrencies(currencies, Some(UsesForeignCurrenciesNo)))),
              bmMsbServices
            )
            .rows

          assertRowMatches(
            14,
            "msb.deal_foreign_currencies.title",
            booleanToLabel(false),
            controllers.msb.routes.UsesForeignCurrenciesController.get(true).url,
            "usesForeignCurrencies-edit"
          )
        }
      }

      "for Money Sources row" must {

        "display correctly for multiple sources" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model, bmMsbServices).rows

          assertRowMatches(
            15,
            "msb.supply_foreign_currencies.cya",
            toBulletList(fullMoneySourcesModel.toMessages),
            controllers.msb.routes.MoneySourcesController.get(true).url,
            "moneysources-edit"
          )
        }

        "display correctly for a single source" in new RowFixture {

          val moneySourcesModel = MoneySources(None, None, Some(true))

          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model.copy(whichCurrencies =
                Some(WhichCurrencies(currencies, Some(UsesForeignCurrenciesYes), Some(moneySourcesModel)))
              ),
              bmMsbServices
            )
            .rows

          assertRowMatches(
            15,
            "msb.supply_foreign_currencies.cya",
            moneySourcesModel.toMessages.mkString,
            controllers.msb.routes.MoneySourcesController.get(true).url,
            "moneysources-edit"
          )
        }
      }

      "for Bank Money Source row" must {

        "display correctly" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model, bmMsbServices).rows

          assertRowMatches(
            16,
            "msb.bank.names",
            bankName,
            controllers.msb.routes.MoneySourcesController.get(true).url,
            "moneysources-banks-edit"
          )
        }
      }

      "for Wholesaler Money Source row" must {

        "display correctly" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model, bmMsbServices).rows

          assertRowMatches(
            17,
            "msb.wholesaler.names",
            wholesalerName,
            controllers.msb.routes.MoneySourcesController.get(true).url,
            "moneysources-wholesalers-edit"
          )
        }
      }
    }

    "not display Transmitting Money Rows" when {

      "Transmitting Money was not selected in MSB services" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper
          .getSummaryList(
            model,
            bmMsbServices.copy(BusinessMatchingMsbServices.all.filterNot(_ == TransmittingMoney).toSet)
          )
          .rows

        assertRowIsNotPresent("msb.ipsp.title")
        assertRowIsNotPresent("msb.summary.ipsp-name")
        assertRowIsNotPresent("msb.summary.ipsp-registration-number")
        assertRowIsNotPresent("msb.fundstransfer.title")
        assertRowIsNotPresent("msb.transactions.expected.cya")
        assertRowIsNotPresent("msb.send.money.title")
        assertRowIsNotPresent("msb.send.the.largest.amounts.of.money.cya")
        assertRowIsNotPresent("msb.most.transactions.cya")
      }
    }

    "not display Currency Exchange Rows" when {

      "Currency Exchange was not selected in MSB services" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper
          .getSummaryList(
            model,
            bmMsbServices.copy(BusinessMatchingMsbServices.all.filterNot(_ == CurrencyExchange).toSet)
          )
          .rows

        assertRowIsNotPresent("msb.ce.transactions.expected.in.12.months.cya")
        assertRowIsNotPresent("msb.which_currencies.cya")
        assertRowIsNotPresent("msb.deal_foreign_currencies.title")
        assertRowIsNotPresent("msb.supply_foreign_currencies.cya")
        assertRowIsNotPresent("msb.bank.names")
        assertRowIsNotPresent("msb.wholesaler.names")
      }
    }

    "not display the Foreign Exchange Row" when {

      "Foreign Exchange was not selected in MSB services" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper
          .getSummaryList(
            model,
            bmMsbServices.copy(BusinessMatchingMsbServices.all.filterNot(_ == ForeignExchange).toSet)
          )
          .rows

        assertRowIsNotPresent("msb.fx.transactions.expected.in.12.months.cya")
      }
    }

    "not display Money Sources rows" when {

      "uses foreign currencies is false" in new RowFixture {
        override val summaryListRows: Seq[Aliases.SummaryListRow] = cyaHelper
          .getSummaryList(
            model.copy(whichCurrencies =
              Some(
                WhichCurrencies(
                  currencies,
                  Some(UsesForeignCurrenciesNo),
                  Some(fullMoneySourcesModel)
                )
              )
            ),
            bmMsbServices
          )
          .rows

        assertRowIsNotPresent("msb.supply_foreign_currencies.cya")
      }
    }
  }
}
