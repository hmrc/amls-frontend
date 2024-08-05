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

package utils.renewal

import models.businessmatching.BusinessActivity.{AccountancyServices, HighValueDealing}
import models.businessmatching.BusinessMatching
import models.businessmatching.BusinessMatchingMsbService.{CurrencyExchange, ForeignExchange, TransmittingMoney}
import models.renewal._
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{SummaryList, SummaryListRow, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, Value}
import utils.CheckYourAnswersHelperFunctions

import javax.inject.Inject

class CheckYourAnswersHelper @Inject()() extends CheckYourAnswersHelperFunctions {

  def getSummaryList(model: Renewal, businessMatching: BusinessMatching)(implicit messages: Messages): SummaryList = {

    val containsASPOrHVD = businessMatching.activities.exists { activities =>
      activities.businessActivities.contains(AccountancyServices) || activities.businessActivities.contains(HighValueDealing)
    }

    SummaryList(
      involvedInOtherActivitiesRows(model).getOrElse(Seq.empty) ++
      Seq(
        businessTurnoverRow(model),
        turnoverRow(model, businessMatching.alphabeticalBusinessActivitiesLowerCase()),
        ampTurnoverRow(model)
      ).flatten ++
      msbServicesRows(model).getOrElse(Seq.empty) ++
        businessMatching.msbServices.fold(Seq.empty[SummaryListRow]){ services =>
        (if (services.msbServices.contains(TransmittingMoney)) getTransmittingMoneyRows(model) else None).getOrElse(Seq.empty) ++
        (if (services.msbServices.contains(CurrencyExchange)) getCurrencyExchangeRows(model) else None).getOrElse(Seq.empty) ++
        (if (services.msbServices.contains(ForeignExchange)) getForeignExchangeRow(model) else None).getOrElse(Seq.empty)
      } ++
      getCustomersOutsideUKRows(model, containsASPOrHVD).getOrElse(Seq.empty) ++
      Seq(getPercentageOfCashRow(model)).flatten ++
      getCashPaymentRows(model).getOrElse(Seq.empty)
    )
  }

  private def involvedInOtherActivitiesRows(model: Renewal)(implicit messages: Messages): Option[Seq[SummaryListRow]] = {

    def booleanRow(bool: Boolean): SummaryListRow =
      row(
        "renewal.involvedinother.title",
        booleanToLabel(bool),
        editAction(
          controllers.renewal.routes.InvolvedInOtherController.get(true).url,
          "renewal.checkYourAnswers.change.busotherActivities",
          "involvedinotheractivities-edit"
        )
      )

    model.involvedInOtherActivities.map {
      case InvolvedInOtherYes(details) =>
        Seq(
          booleanRow(true),
          row(
            "renewal.involvedinother.cya.second.title",
            details,
            editAction(
              controllers.renewal.routes.InvolvedInOtherController.get(true).url,
              "renewal.checkYourAnswers.change.otherActivitiesDesc",
              "involvedinotheractivities-details-edit"
            )
          )
        )
      case InvolvedInOtherNo =>
        Seq(booleanRow(false))
    }
  }

  private def businessTurnoverRow(model: Renewal)(implicit messages: Messages): Option[SummaryListRow] = {

    model.businessTurnover.map { businessTurnover =>
      row(
        "renewal.business-turnover.title",
        messages(s"businessactivities.turnover.lbl.${businessTurnover.value}"),
        editAction(
          controllers.renewal.routes.BusinessTurnoverController.get(true).url,
          "renewal.checkYourAnswers.change.turnover",
          "businessturnover-edit"
        )
      )
    }
  }

  private def turnoverRow(model: Renewal, activities: Option[List[String]])(implicit messages: Messages): Option[SummaryListRow] = {

    def turnoverRow(title: String, answer: String): SummaryListRow =
      row(
        title,
        messages(s"businessactivities.business-turnover.lbl.$answer"),
        editAction(
          controllers.renewal.routes.AMLSTurnoverController.get(true).url,
          "renewal.checkYourAnswers.change.ampTurnover",
          "turnover-edit"
        )
      )

    model.turnover flatMap { turnover =>
      activities map {
        case activity :: Nil => turnoverRow(
          messages("renewal.turnover.title", activity), turnover.value
        )
        case _ => turnoverRow("renewal.turnover.title", turnover.value)
      }
    }
  }

  private def ampTurnoverRow(model: Renewal)(implicit messages: Messages): Option[SummaryListRow] = {

    model.ampTurnover.map { ampTurnover =>
      row(
        "renewal.amp.turnover.title",
        messages(s"hvd.percentage.lbl.${ampTurnover.value}"),
        editAction(
          controllers.renewal.routes.AMPTurnoverController.get(true).url,
          "renewal.checkYourAnswers.change.ampTurnover",
          "ampTurnover-edit"
        )
      )
    }
  }

  private def msbServicesRows(model: Renewal)(implicit messages: Messages): Option[Seq[SummaryListRow]] = {

    val seq = Seq(
      model.totalThroughput.map { totalThroughput =>
        row(
          "renewal.msb.throughput.header",
          TotalThroughput.labelFor(totalThroughput),
          editAction(
            controllers.renewal.routes.TotalThroughputController.get(true).url,
            "renewal.checkYourAnswers.change.totalThroughput",
            "msbtotalthroughput-edit"
          )
        )
      },
      model.transactionsInLast12Months.map { transactions =>
        row(
          "renewal.msb.transfers.header",
          transactions.transfers,
          editAction(
            controllers.renewal.routes.TransactionsInLast12MonthsController.get(true).url,
            "renewal.checkYourAnswers.change.totalMoneyTrans",
            "msbtransfers-edit"
          )
        )
      }
    ).flatten

    if(seq.nonEmpty) Some(seq) else None
  }

  private def getTransmittingMoneyRows(model: Renewal)(implicit messages: Messages): Option[Seq[SummaryListRow]] = {
    val seq = Seq(
      model.sendMoneyToOtherCountry.map { otherCountry =>
        row(
          "renewal.msb.send.money.title",
          booleanToLabel(otherCountry.money),
          editAction(
            controllers.renewal.routes.SendMoneyToOtherCountryController.get(edit = true).url,
            "renewal.checkYourAnswers.change.transToOtherCountry",
            "msbsendmoney-edit"
          )
        )
      },
      model.sendTheLargestAmountsOfMoney.map { lom =>
        SummaryListRow(
          Key(Text(messages("renewal.msb.largest.amounts.title"))),
          if (lom.countries.size == 1) {
            Value(Text(lom.countries.head.name))
          } else {
            toBulletList(lom.countries.map(_.name))
          },
          actions = editAction(
            controllers.renewal.routes.SendTheLargestAmountsOfMoneyController.get(edit = true).url,
            "renewal.checkYourAnswers.change.whichCountries",
            "msblargestamounts-edit"
          )
        )
      },
      model.mostTransactions.map { mt =>
        SummaryListRow(
          Key(Text(messages("renewal.msb.most.transactions.title"))),
          if (mt.countries.size == 1) {
            Value(Text(mt.countries.head.name))
          } else {
            toBulletList(mt.countries.map(_.name))
          },
          actions = editAction(
            controllers.renewal.routes.MostTransactionsController.get(edit = true).url,
            "renewal.checkYourAnswers.change.whereSentMost",
            "msbmostransactions-edit"
          )
        )
      }
    ).flatten

    if(seq.nonEmpty) Some(seq) else None
  }

  private def getCurrencyExchangeRows(model: Renewal)(implicit messages: Messages): Option[Seq[SummaryListRow]] = {

    def getMoneySourcesRow(value: Value) = {
      SummaryListRow(
        Key(Text(messages("renewal.msb.money_sources.header"))),
        value,
        actions = editAction(
          controllers.renewal.routes.MoneySourcesController.get(edit = true).url,
          "renewal.checkYourAnswers.change.whoSuppliedFCurrency",
          "msbmoneysources-edit"
        )
      )
    }

    model.whichCurrencies map { wc =>
      Seq(
        model.ceTransactionsInLast12Months map { transactions =>
          row(
            "renewal.msb.ce.transactions.expected.title",
            transactions.ceTransaction,
            editAction(
              controllers.renewal.routes.CETransactionsInLast12MonthsController.get(edit = true).url,
              "renewal.checkYourAnswers.change.whereSentMost",
              "msbcetransactionsexpected-edit"
            )
          )
        },
        Some(
          SummaryListRow(
            Key(Text(messages("renewal.msb.whichcurrencies.header"))),
            if (wc.currencies.size == 1) {
              Value(Text(wc.currencies.head))
            } else {
              toBulletList(wc.currencies)
            },
            actions = editAction(
              controllers.renewal.routes.WhichCurrenciesController.get(edit = true).url,
              "renewal.checkYourAnswers.change.currencySupplied",
              "msbwhichcurrencies-edit"
            )
          )
        ),
        wc.usesForeignCurrencies map { ufc =>
          row(
            "renewal.msb.foreign_currencies.header",
            ufc match {
              case UsesForeignCurrenciesYes => booleanToLabel(true)
              case UsesForeignCurrenciesNo => booleanToLabel(false)
            },
            editAction(
              controllers.renewal.routes.UsesForeignCurrenciesController.get(true).url,
              "renewal.checkYourAnswers.change.currencySupplied",
              "msbusesforeigncurrencies-edit"
            )
          )
        }
      ).flatten ++
        wc.moneySources.map { ms =>
          Seq(ms match {
            case ms if ms.size == 1 => Some(getMoneySourcesRow(Value(Text(ms.toMessages.mkString))))
            case ms if ms.size > 1 => Some(getMoneySourcesRow(toBulletList(ms.toMessages)))
            case _ => None
          }).flatten ++ Seq(
            ms.bankMoneySource map { source =>
              row(
                "msb.which_currencies.source.which_banks",
                source.bankNames,
                editAction(
                  controllers.renewal.routes.MoneySourcesController.get(true).url,
                  "renewal.checkYourAnswers.change.whoSuppliedFCurrency",
                  "msbbankmoneysources-edit"
                )
              )
            },
            ms.wholesalerMoneySource map { source =>
              row(
                "msb.which_currencies.source.which_wholesalers",
                source.wholesalerNames,
                editAction(
                  controllers.renewal.routes.MoneySourcesController.get(true).url,
                  "renewal.checkYourAnswers.change.whoSuppliedFCurrency",
                  "msbwholesalermoneysources-edit"
                )
              )
            }
          ).flatten
        }.getOrElse(Seq.empty[SummaryListRow])
    }
  }

  private def getForeignExchangeRow(model: Renewal)(implicit messages: Messages): Option[Seq[SummaryListRow]] = {
    model.fxTransactionsInLast12Months.map { transactions =>
      Seq(
        row(
          "renewal.msb.fx.transactions.expected.title",
          transactions.fxTransaction,
          editAction(
            controllers.renewal.routes.FXTransactionsInLast12MonthsController.get(edit = true).url,
            "renewal.checkYourAnswers.change.howManyFExchangeTrans",
            "msbfxtransactionsexpected-edit"
          )
        )
      )
    }
  }

  private def getCustomersOutsideUKRows(model: Renewal, showOutsideUKRow: Boolean)(implicit messages: Messages): Option[Seq[SummaryListRow]] = {
    val seq = Seq(
      model.customersOutsideIsUK.map { boa =>
        row(
          "renewal.customer.outside.uk.title",
          booleanToLabel(boa.isOutside),
          editAction(
            controllers.renewal.routes.CustomersOutsideIsUKController.get(true).url,
            "renewal.checkYourAnswers.change.custOutsideUK",
            "customersoutsideisuk-edit"
          )
        )
      },
      model.customersOutsideUK.flatMap { boa =>
        def makeRow(value: Value) = {
          SummaryListRow(
            Key(Text(messages("renewal.customer.outside.uk.countries.title"))),
            value,
            actions = editAction(
              controllers.renewal.routes.CustomersOutsideUKController.get(true).url,
              "renewal.checkYourAnswers.change.whichCountryCust",
              "customersoutsideuk-edit"
            )
          )
        }
        boa.countries match {
          case Some(countries) if showOutsideUKRow && countries.length == 1 => Some(makeRow(Value(Text(countries.head.name))))
          case Some(countries) if showOutsideUKRow => Some(makeRow(toBulletList(countries.map(_.name))))
          case _ => None
        }
      }
    ).flatten

    if(seq.nonEmpty) Some(seq) else None
  }

  private def getPercentageOfCashRow(model: Renewal)(implicit messages: Messages): Option[SummaryListRow] = {
    model.percentageOfCashPaymentOver15000.map { percentage =>
      row(
        "renewal.hvd.percentage.title",
        messages(s"hvd.percentage.lbl.${percentage.value}"),
        editAction(
          controllers.renewal.routes.PercentageOfCashPaymentOver15000Controller.get(true).url,
          "renewal.checkYourAnswers.change.percentageCashPayments",
          "hvdpercentage-edit"
        )
      )
    }
  }

  def getCashPaymentRows(model: Renewal)(implicit messages: Messages): Option[Seq[SummaryListRow]] = {
    model.receiveCashPayments.map { rcp =>
      Seq(
        Some(
          row(
            "renewal.receiving.title",
            rcp.cashPaymentsCustomerNotMet match {
              case CashPaymentsCustomerNotMet(false) => {
                booleanToLabel(false)
              }
              case CashPaymentsCustomerNotMet(true) => {
                booleanToLabel(true)
              }
            },
            editAction(
              controllers.renewal.routes.CashPaymentsCustomersNotMetController.get(true).url,
              "renewal.checkYourAnswers.change.receivedCashPayments",
              "receivecashpayments-edit"
            )
          )
        ),
        rcp.howCashPaymentsReceived map { hcpr =>
          SummaryListRow(
            Key(Text(messages("renewal.cash.payments.received.title"))),
            hcpr.paymentMethods.getSummaryMessages match {
              case message :: Nil => Value(Text(message))
              case messageList => toBulletList(messageList)
            },
            actions = editAction(
              controllers.renewal.routes.HowCashPaymentsReceivedController.get(true).url,
              "renewal.checkYourAnswers.change.howReceivedCashPayments",
              "receivecashpaymentshowreceived-edit"
            )
          )
        }
      ).flatten
    }
  }
}