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
import models.businessmatching.BusinessMatchingMsbServices
import models.moneyservicebusiness._
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{SummaryList, SummaryListRow, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Key
import utils.CheckYourAnswersHelperFunctions

import javax.inject.Inject

class CheckYourAnswersHelper @Inject()() extends CheckYourAnswersHelperFunctions {

  def getSummaryList(model: MoneyServiceBusiness, bmMsbServices: BusinessMatchingMsbServices)(implicit messages: Messages): SummaryList = {

    val transmittingMoneyRows = if(bmMsbServices.msbServices.contains(TransmittingMoney)) {
      businessUseAnIPSPRows(model).getOrElse(Seq.empty[SummaryListRow]) ++
      Seq(
        fundsTransferRow(model),
        transactionsInNext12MonthsRow(model)
      ).flatten ++
      sendMoneyToOtherCountryRows(model).getOrElse(Seq.empty[SummaryListRow])
    } else {
      Seq.empty[SummaryListRow]
    }

    val currencyExchangeRows = if(bmMsbServices.msbServices.contains(CurrencyExchange)) {
      Seq(
        ceTransactionsInNext12MonthsRow(model)
      ).flatten ++
        whichCurrenciesRows(model).getOrElse(Seq.empty[SummaryListRow])
    } else {
      Seq.empty[SummaryListRow]
    }

    val foreignExchangeRow = if(bmMsbServices.msbServices.contains(ForeignExchange)) {
      Seq(fxTransactionsInNext12MonthsRow(model)).flatten
    } else {
      Seq.empty[SummaryListRow]
    }

    SummaryList(
      Seq(expectedThroughputRow(model)).flatten ++
        branchesOrAgentsRows(model).getOrElse(Seq.empty[SummaryListRow]) ++
      Seq(identifyLinkedTransactionsRow(model)).flatten ++
        transmittingMoneyRows ++
        currencyExchangeRows ++
        foreignExchangeRow
    )
  }

  private def expectedThroughputRow(model: MoneyServiceBusiness)(implicit messages: Messages): Option[SummaryListRow] = {

    model.throughput.map { expectedThroughput =>
      row(
        "msb.throughput.title",
        messages(s"msb.throughput.lbl.${expectedThroughput.value}"),
        editAction(
          controllers.msb.routes.ExpectedThroughputController.get(true).url,
          "msb.checkYourAnswers.change.totalTransValue",
          "msbthroughput-edit"
        )
      )
    }
  }

  private def branchesOrAgentsRows(model: MoneyServiceBusiness)(implicit messages: Messages): Option[Seq[SummaryListRow]] = {

    model.branchesOrAgents.map {
      case BranchesOrAgents(BranchesOrAgentsHasCountries(true), Some(branches)) =>
        Seq(
          row(
            "msb.branchesoragents.title",
            booleanToLabel(true),
            editAction(
              controllers.msb.routes.BranchesOrAgentsController.get(true).url,
              "msb.checkYourAnswers.change.branchesInOtherCountries",
              "msbbranchesoragents-edit"
            )
          ),
          SummaryListRow(
            Key(Text(messages("msb.branchesoragents.countries.title"))),
            branches.branches match {
              case branch :: Nil => Value(Text(branch.name))
              case branches => toBulletList(branches)
            },
            actions = editAction(
              controllers.msb.routes.BranchesOrAgentsWhichCountriesController.get(true).url,
              "msb.checkYourAnswers.change.branchesOrAgentsCountries",
              "msbwhichcountries-edit"
            )
          )
        )
      case BranchesOrAgents(BranchesOrAgentsHasCountries(false), _) =>
        Seq(
          row(
            "msb.branchesoragents.title",
            booleanToLabel(false),
            editAction(
              controllers.msb.routes.BranchesOrAgentsController.get(true).url,
              "msb.checkYourAnswers.change.branchesOrAgentsCountries",
              "msbbranchesoragents-edit"
            )
          )
        )
    }
  }

  private def identifyLinkedTransactionsRow(model: MoneyServiceBusiness)(implicit messages: Messages): Option[SummaryListRow] = {
    model.identifyLinkedTransactions.map { ilt =>
      row(
        "msb.linked.txn.title",
        booleanToLabel(ilt.linkedTxn),
        editAction(
          controllers.msb.routes.IdentifyLinkedTransactionsController.get(true).url,
          "msb.checkYourAnswers.change.canIdentifyLinkedTransactions",
          "msblinkedtransactions-edit"
        )
      )
    }
  }

  private def businessUseAnIPSPRows(model: MoneyServiceBusiness)(implicit messages: Messages): Option[Seq[SummaryListRow]] = {

    model.businessUseAnIPSP.map {
      case BusinessUseAnIPSPNo =>
        Seq(
          row(
            "msb.ipsp.title",
            booleanToLabel(false),
            editAction(
              controllers.msb.routes.BusinessUseAnIPSPController.get(true).url,
              "msb.checkYourAnswers.change.ISPUsed",
              "msbipsp-edit"
            )
          )
        )
      case BusinessUseAnIPSPYes(name, reference) =>
        Seq(
          row(
            "msb.ipsp.title",
            booleanToLabel(true),
            editAction(
              controllers.msb.routes.BusinessUseAnIPSPController.get(true).url,
              "msb.checkYourAnswers.change.ISPUsed",
              "msbipsp-edit"
            )
          ),
          row(
            "msb.summary.ipsp-name",
            name,
            editAction(
              controllers.msb.routes.BusinessUseAnIPSPController.get(true).url,
              "msb.checkYourAnswers.change.ISPSname",
              "msbipsp-name-edit"
            )
          ),
          row(
            "msb.summary.ipsp-registration-number",
            reference,
            editAction(
              controllers.msb.routes.BusinessUseAnIPSPController.get(true).url,
              "msb.checkYourAnswers.change.ISPSMLReregNo",
              "msbipsp-number-edit"
            )
          )
        )
    }
  }

  private def fundsTransferRow(model: MoneyServiceBusiness)(implicit messages: Messages): Option[SummaryListRow] = {
    model.fundsTransfer.map { ft =>
      row(
        "msb.fundstransfer.title",
        booleanToLabel(ft.transferWithoutFormalSystems),
        editAction(
          controllers.msb.routes.FundsTransferController.get(true).url,
          "msb.checkYourAnswers.change.transferWithoutFormalBanking",
          "msbfundstransfer-edit"
        )
      )
    }
  }

  private def transactionsInNext12MonthsRow(model: MoneyServiceBusiness)(implicit messages: Messages): Option[SummaryListRow] = {
    model.transactionsInNext12Months.map { transactions =>
      row(
        "msb.transactions.expected.title",
        transactions.txnAmount,
        editAction(
          controllers.msb.routes.TransactionsInNext12MonthsController.get(true).url,
          "msb.checkYourAnswers.change.transfersExpected",
          "msbtransactionsexpected-edit"
        )
      )
    }
  }

  private def sendMoneyToOtherCountryRows(model: MoneyServiceBusiness)(implicit messages: Messages): Option[Seq[SummaryListRow]] = {

    model.sendMoneyToOtherCountry.map {
      case SendMoneyToOtherCountry(money) if !money =>
        Seq(
          row(
            "msb.send.money.title",
            booleanToLabel(false),
            editAction(
              controllers.msb.routes.SendMoneyToOtherCountryController.get(true).url,
              "msb.checkYourAnswers.change.sendToOtherCountry",
              "msbsendmoneytoothercountries-edit"
            )
          )
        )
      case SendMoneyToOtherCountry(money) if money =>
        Seq(
          Some(row(
            "msb.send.money.title",
            booleanToLabel(true),
            editAction(
              controllers.msb.routes.SendMoneyToOtherCountryController.get(true).url,
              "msb.checkYourAnswers.change.sendToOtherCountry",
              "msbsendmoneytoothercountries-edit"
            )
          )),
          model.sendTheLargestAmountsOfMoney.map { amount =>
            SummaryListRow(
              Key(Text(messages("msb.send.the.largest.amounts.of.money.title"))),
              amount.countries match {
                case country :: Nil => Value(Text(country.name))
                case countries => toBulletList(countries)
              },
              actions = editAction(
                controllers.msb.routes.SendTheLargestAmountsOfMoneyController.get(true).url,
                "msb.checkYourAnswers.change.countryLargestAmounts",
                "msbsendlargestamounts-edit"
              )
            )
          },
          model.mostTransactions.map { mt =>
            SummaryListRow(
              Key(Text(messages("msb.most.transactions.title"))),
              mt.countries match {
                case country :: Nil => Value(Text(country.name))
                case countries => toBulletList(countries)
              },
              actions = editAction(
                controllers.msb.routes.MostTransactionsController.get(true).url,
                "msb.checkYourAnswers.change.countryMostTransactions",
                "msbmosttransactions-edit"
              )
            )
          }
        ).flatten
    }
  }

  private def ceTransactionsInNext12MonthsRow(model: MoneyServiceBusiness)(implicit messages: Messages): Option[SummaryListRow] = {
    model.ceTransactionsInNext12Months.map { transactions =>
      row(
        "msb.ce.transactions.expected.in.12.months.title",
        transactions.ceTransaction,
        editAction(
          controllers.msb.routes.CurrencyExchangesInNext12MonthsController.get(true).url,
          "msb.checkYourAnswers.change.currencyExchangeTransactionsExpected",
          "msbcetransactionsexpected-edit"
        )
      )
    }
  }

  private def whichCurrenciesRows(model: MoneyServiceBusiness)(implicit messages: Messages): Option[Seq[SummaryListRow]] = {

    model.whichCurrencies.map {
      case WhichCurrencies(currencies, usesForeignCurrencies, moneySources) =>
        Seq(
          Some(SummaryListRow(
            Key(Text(messages("msb.which_currencies.title"))),
            currencies match {
              case currency :: Nil => Value(Text(currency))
              case currencies => toBulletList(currencies)
            },
            actions = editAction(
              controllers.msb.routes.WhichCurrenciesController.get(true).url,
              "msb.checkYourAnswers.change.currenciesExpectToSupply",
              "whichCurrencies-edit"
            )
          )),
          usesForeignCurrencies map { foreignCurrencies =>
            row(
              "msb.deal_foreign_currencies.title",
              booleanToLabel(foreignCurrencies.value),
              editAction(
                controllers.msb.routes.UsesForeignCurrenciesController.get(true).url,
                "msb.checkYourAnswers.change.physicalForeignCurrencies",
                "usesForeignCurrencies-edit"
              )
            )
          }
        ).flatten ++
        moneySources.fold(Seq.empty[SummaryListRow]) { sources =>
          Seq(
            usesForeignCurrencies flatMap { foreignCurrencies =>
              if(foreignCurrencies.value) {
                Some(SummaryListRow(
                  Key(Text(messages("msb.supply_foreign_currencies.title"))),
                  sources.toMessages match {
                    case source :: Nil => Value(Text(source))
                    case sources => toBulletList(sources)
                  },
                  actions = editAction(
                    controllers.msb.routes.MoneySourcesController.get(true).url,
                    "msb.checkYourAnswers.change.whoSupplyForeignCurrency",
                    "moneysources-edit"
                  )
                ))
              } else None
            },
            sources.bankMoneySource map { bms =>
              row(
                "msb.bank.names",
                bms.bankNames,
                editAction(
                  controllers.msb.routes.MoneySourcesController.get(true).url,
                  "msb.checkYourAnswers.change.banksSupplyForeignCurrency",
                  "moneysources-banks-edit"
                )
              )
            },
            sources.wholesalerMoneySource map { wms =>
              row(
                "msb.wholesaler.names",
                wms.wholesalerNames,
                editAction(
                  controllers.msb.routes.MoneySourcesController.get(true).url,
                  "msb.checkYourAnswers.change.wholesalersSupplyForeignCurrency",
                  "moneysources-wholesalers-edit"
                )
              )
            }
          ).flatten
        }
    }
  }

  private def fxTransactionsInNext12MonthsRow(model: MoneyServiceBusiness)(implicit messages: Messages): Option[SummaryListRow] = {
    model.fxTransactionsInNext12Months.map { transactions =>
      row(
        "msb.fx.transactions.expected.in.12.months.title",
        transactions.fxTransaction,
        editAction(
          controllers.msb.routes.FXTransactionsInNext12MonthsController.get(true).url,
          "msb.checkYourAnswers.change.foreignExchangeTransactionsExpected",
          "msbfxtransactionsexpected-edit"
        )
      )
    }
  }
}
