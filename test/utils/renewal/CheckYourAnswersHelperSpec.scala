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

import models.Country
import models.businessmatching.BusinessActivity.{AccountancyServices, HighValueDealing}
import models.businessmatching.BusinessMatchingMsbService.{CurrencyExchange, TransmittingMoney}
import models.businessmatching.{BusinessActivities, BusinessActivity, BusinessMatching, BusinessMatchingMsbService, BusinessMatchingMsbServices}
import models.renewal._
import org.scalatest.Assertion
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryListRow
import utils.AmlsSpec

class CheckYourAnswersHelperSpec extends AmlsSpec {

  val cyaHelper: CheckYourAnswersHelper = new CheckYourAnswersHelper()

  val countries: Seq[Country] = Seq(
    Country("United Kingdom", "GB"),
    Country("United States", "US"),
    Country("Germany", "DE")
  )

  val currencies: Seq[String] = Seq("GBP", "USD", "JPY")

  val activity                   = "A description of activities"
  val transactionsInLast12Months = "1500"
  val ceTransactions             = "123"
  val fxTransactions             = "12"
  val bankName                   = "Bank Name"
  val wholesalerName             = "Wholesaler Name"
  val otherPaymentMethod         = "Third party"

  val moneySourcesObj = MoneySources(
    Some(BankMoneySource(bankName)),
    Some(WholesalerMoneySource(wholesalerName)),
    Some(true)
  )

  val paymentMethods = PaymentMethods(true, true, Some(otherPaymentMethod))

  val model: Renewal = Renewal(
    Some(InvolvedInOtherYes(activity)),
    Some(BusinessTurnover.First),
    Some(AMLSTurnover.First),
    Some(AMPTurnover.First),
    Some(CustomersOutsideIsUK(true)),
    Some(CustomersOutsideUK(Some(countries))),
    Some(PercentageOfCashPaymentOver15000.First),
    Some(
      CashPayments(
        CashPaymentsCustomerNotMet(true),
        Some(HowCashPaymentsReceived(paymentMethods))
      )
    ),
    Some(TotalThroughput("01")),
    Some(
      WhichCurrencies(currencies, Some(UsesForeignCurrenciesYes), Some(moneySourcesObj))
    ),
    Some(TransactionsInLast12Months(transactionsInLast12Months)),
    Some(SendTheLargestAmountsOfMoney(countries)),
    Some(MostTransactions(countries)),
    Some(CETransactionsInLast12Months(ceTransactions)),
    Some(FXTransactionsInLast12Months(fxTransactions)),
    false,
    Some(SendMoneyToOtherCountry(true)),
    hasAccepted = true
  )

  def businessMatching(
    activities: Set[BusinessActivity] = BusinessActivities.all,
    msbActivities: Set[BusinessMatchingMsbService] = BusinessMatchingMsbServices.all.toSet
  ): BusinessMatching =
    BusinessMatching(
      activities = Some(BusinessActivities(activities)),
      msbServices = Some(BusinessMatchingMsbServices(msbActivities))
    )

  val activities: Option[List[String]] = businessMatching().alphabeticalBusinessActivitiesLowerCase()

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

      "for the Involved in others rows" must {

        "render correctly when answer is Yes" in new RowFixture {
          override val summaryListRows: Seq[Aliases.SummaryListRow] = cyaHelper
            .getSummaryList(
              model,
              businessMatching()
            )
            .rows

          assertRowMatches(
            0,
            "renewal.involvedinother.title",
            booleanToLabel(true),
            controllers.renewal.routes.InvolvedInOtherController.get(true).url,
            "involvedinotheractivities-edit"
          )

          assertRowMatches(
            1,
            "renewal.involvedinother.cya.second.title",
            activity,
            controllers.renewal.routes.InvolvedInOtherController.get(true).url,
            "involvedinotheractivities-details-edit"
          )
        }

        "render correctly when answer is No" in new RowFixture {
          override val summaryListRows: Seq[Aliases.SummaryListRow] = cyaHelper
            .getSummaryList(
              model.copy(involvedInOtherActivities = Some(InvolvedInOtherNo)),
              businessMatching()
            )
            .rows

          assertRowMatches(
            0,
            "renewal.involvedinother.title",
            booleanToLabel(false),
            controllers.renewal.routes.InvolvedInOtherController.get(true).url,
            "involvedinotheractivities-edit"
          )
        }
      }

      "for the Business turnover row" must {

        BusinessTurnover.all foreach { businessTurnover =>
          s"display for ${businessTurnover.toString} radio" in new RowFixture {
            override val summaryListRows: Seq[SummaryListRow] = cyaHelper
              .getSummaryList(
                model.copy(businessTurnover = Some(businessTurnover)),
                businessMatching()
              )
              .rows

            assertRowMatches(
              2,
              "renewal.business-turnover.title",
              messages(s"businessactivities.turnover.lbl.${businessTurnover.value}"),
              controllers.renewal.routes.BusinessTurnoverController.get(true).url,
              "businessturnover-edit"
            )
          }
        }
      }

      "for the Turnover rows" must {

        AMLSTurnover.all foreach { turnover =>
          s"display for ${turnover.toString} radio for single service" in new RowFixture {

            val bmWithSingleActivity: BusinessMatching = businessMatching(Set(BusinessActivities.all.head))

            val singleActivity: Option[List[String]] = bmWithSingleActivity.alphabeticalBusinessActivitiesLowerCase()

            override val summaryListRows: Seq[SummaryListRow] = cyaHelper
              .getSummaryList(
                model.copy(turnover = Some(turnover)),
                bmWithSingleActivity
              )
              .rows

            assertRowMatches(
              3,
              messages("renewal.turnover.title", singleActivity.get.head),
              messages(s"businessactivities.business-turnover.lbl.${turnover.value}"),
              controllers.renewal.routes.AMLSTurnoverController.get(true).url,
              "turnover-edit"
            )
          }

          s"display for ${turnover.toString} radio for multiple services" in new RowFixture {

            override val summaryListRows: Seq[SummaryListRow] = cyaHelper
              .getSummaryList(
                model.copy(turnover = Some(turnover)),
                businessMatching()
              )
              .rows

            assertRowMatches(
              3,
              "renewal.turnover.title",
              messages(s"businessactivities.business-turnover.lbl.${turnover.value}"),
              controllers.renewal.routes.AMLSTurnoverController.get(true).url,
              "turnover-edit"
            )
          }
        }
      }

      "for the AMP turnover row" must {

        AMPTurnover.all foreach { ampTurnover =>
          s"display for ${ampTurnover.toString} radio" in new RowFixture {
            override val summaryListRows: Seq[SummaryListRow] = cyaHelper
              .getSummaryList(
                model.copy(ampTurnover = Some(ampTurnover)),
                businessMatching()
              )
              .rows

            assertRowMatches(
              4,
              "renewal.amp.turnover.title",
              messages(s"hvd.percentage.lbl.${ampTurnover.value}"),
              controllers.renewal.routes.AMPTurnoverController.get(true).url,
              "ampTurnover-edit"
            )
          }
        }
      }

      "for the Total throughput row" must {

        TotalThroughput.throughputValues.map(_.value).zip(TotalThroughput.throughputValues.map(_.label)).foreach {
          case (value, label) =>
            s"display for ${messages(label)} radio" in new RowFixture {
              override val summaryListRows: Seq[SummaryListRow] = cyaHelper
                .getSummaryList(
                  model.copy(totalThroughput = Some(TotalThroughput(value))),
                  businessMatching()
                )
                .rows

              assertRowMatches(
                5,
                "renewal.msb.throughput.header",
                TotalThroughput.labelFor(TotalThroughput(value)),
                controllers.renewal.routes.TotalThroughputController.get(true).url,
                "msbtotalthroughput-edit"
              )
            }
        }
      }

      "for the Transactions in last 12 months row" must {

        "display correct row for amount transferred" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model,
              businessMatching()
            )
            .rows

          assertRowMatches(
            6,
            "renewal.msb.transfers.header",
            transactionsInLast12Months,
            controllers.renewal.routes.TransactionsInLast12MonthsController.get(true).url,
            "msbtransfers-edit"
          )
        }
      }

      "for the Send money to other countries row" must {

        "render correctly when answer is Yes" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model,
              businessMatching()
            )
            .rows

          assertRowMatches(
            7,
            "renewal.msb.send.money.title",
            booleanToLabel(true),
            controllers.renewal.routes.SendMoneyToOtherCountryController.get(edit = true).url,
            "msbsendmoney-edit"
          )
        }

        "render correctly when answer is No" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model.copy(sendMoneyToOtherCountry = Some(SendMoneyToOtherCountry(false))),
              businessMatching()
            )
            .rows

          assertRowMatches(
            7,
            "renewal.msb.send.money.title",
            booleanToLabel(false),
            controllers.renewal.routes.SendMoneyToOtherCountryController.get(edit = true).url,
            "msbsendmoney-edit"
          )
        }
      }

      "for the Send largest amount of money row" must {

        s"display correctly for a single country" in new RowFixture {

          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model.copy(sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(Seq(countries.head)))),
              businessMatching()
            )
            .rows

          assertRowMatches(
            8,
            messages("renewal.msb.largest.amounts.title"),
            countries.head.name,
            controllers.renewal.routes.SendTheLargestAmountsOfMoneyController.get(edit = true).url,
            "msblargestamounts-edit"
          )
        }

        s"display correctly for multiple countries" in new RowFixture {

          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model,
              businessMatching()
            )
            .rows

          assertRowMatches(
            8,
            messages("renewal.msb.largest.amounts.title"),
            toBulletList(countries.map(_.name)),
            controllers.renewal.routes.SendTheLargestAmountsOfMoneyController.get(edit = true).url,
            "msblargestamounts-edit"
          )
        }
      }

      "for the Most transactions row" must {

        s"display correctly for a single country" in new RowFixture {

          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model.copy(mostTransactions = Some(MostTransactions(Seq(countries.head)))),
              businessMatching()
            )
            .rows

          assertRowMatches(
            9,
            messages("renewal.msb.most.transactions.title"),
            countries.head.name,
            controllers.renewal.routes.MostTransactionsController.get(edit = true).url,
            "msbmostransactions-edit"
          )
        }

        s"display correctly for multiple countries" in new RowFixture {

          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model,
              businessMatching()
            )
            .rows

          assertRowMatches(
            9,
            messages("renewal.msb.most.transactions.title"),
            toBulletList(countries.map(_.name)),
            controllers.renewal.routes.MostTransactionsController.get(edit = true).url,
            "msbmostransactions-edit"
          )
        }
      }

      "for the Currency Exchange transactions in last 12 months row" must {

        "display correct row for amount transferred" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model,
              businessMatching()
            )
            .rows

          assertRowMatches(
            10,
            "renewal.msb.ce.transactions.expected.title",
            ceTransactions,
            controllers.renewal.routes.CETransactionsInLast12MonthsController.get(edit = true).url,
            "msbcetransactionsexpected-edit"
          )
        }
      }

      "for Which currencies row" must {

        s"display correctly for a single currency" in new RowFixture {

          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model.copy(whichCurrencies =
                Some(WhichCurrencies(Seq(currencies.head), Some(UsesForeignCurrenciesYes), Some(moneySourcesObj)))
              ),
              businessMatching()
            )
            .rows

          assertRowMatches(
            11,
            messages("renewal.msb.whichcurrencies.header"),
            currencies.head,
            controllers.renewal.routes.WhichCurrenciesController.get(edit = true).url,
            "msbwhichcurrencies-edit"
          )
        }

        s"display correctly for multiple currencies" in new RowFixture {

          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model,
              businessMatching()
            )
            .rows

          assertRowMatches(
            11,
            messages("renewal.msb.whichcurrencies.header"),
            toBulletList(currencies),
            controllers.renewal.routes.WhichCurrenciesController.get(edit = true).url,
            "msbwhichcurrencies-edit"
          )
        }
      }

      "for the Uses foreign currencies row" must {

        "render correctly when answer is Yes" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model,
              businessMatching()
            )
            .rows

          assertRowMatches(
            12,
            "renewal.msb.foreign_currencies.header",
            booleanToLabel(true),
            controllers.renewal.routes.UsesForeignCurrenciesController.get(true).url,
            "msbusesforeigncurrencies-edit"
          )
        }

        "render correctly when answer is No" in new RowFixture {
          override val summaryListRows: Seq[Aliases.SummaryListRow] = cyaHelper
            .getSummaryList(
              model.copy(whichCurrencies =
                Some(WhichCurrencies(currencies, Some(UsesForeignCurrenciesNo), Some(moneySourcesObj)))
              ),
              businessMatching()
            )
            .rows

          assertRowMatches(
            12,
            "renewal.msb.foreign_currencies.header",
            booleanToLabel(false),
            controllers.renewal.routes.UsesForeignCurrenciesController.get(true).url,
            "msbusesforeigncurrencies-edit"
          )
        }
      }

      "for the Money sources row" must {

        s"display correctly for a single source" in new RowFixture {

          val moneySources = MoneySources(customerMoneySource = Some(true))

          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model.copy(whichCurrencies =
                Some(WhichCurrencies(Seq(currencies.head), Some(UsesForeignCurrenciesYes), Some(moneySources)))
              ),
              businessMatching()
            )
            .rows

          assertRowMatches(
            13,
            "renewal.msb.money_sources.header",
            moneySources.toMessages.mkString,
            controllers.renewal.routes.MoneySourcesController.get(edit = true).url,
            "msbmoneysources-edit"
          )
        }

        s"display correctly for multiple sources" in new RowFixture {

          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model,
              businessMatching()
            )
            .rows

          assertRowMatches(
            13,
            "renewal.msb.money_sources.header",
            toBulletList(moneySourcesObj.toMessages),
            controllers.renewal.routes.MoneySourcesController.get(edit = true).url,
            "msbmoneysources-edit"
          )
        }
      }

      "for the Bank money source row" must {

        "display correct row when it is present" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model,
              businessMatching()
            )
            .rows

          assertRowMatches(
            14,
            "msb.which_currencies.source.which_banks",
            bankName,
            controllers.renewal.routes.MoneySourcesController.get(true).url,
            "msbbankmoneysources-edit"
          )
        }

        "not display the row if bank source is not present" in new RowFixture {
          val moneySources = MoneySources(None, Some(WholesalerMoneySource(wholesalerName)), Some(true))

          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model.copy(whichCurrencies =
                Some(WhichCurrencies(Seq(currencies.head), Some(UsesForeignCurrenciesYes), Some(moneySources)))
              ),
              businessMatching()
            )
            .rows

          assertRowIsNotPresent("msb.which_currencies.source.which_banks")
        }
      }

      "for the Wholesaler money source row" must {

        "display correct row when it is present" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model,
              businessMatching()
            )
            .rows

          assertRowMatches(
            15,
            "msb.which_currencies.source.which_wholesalers",
            wholesalerName,
            controllers.renewal.routes.MoneySourcesController.get(true).url,
            "msbwholesalermoneysources-edit"
          )
        }

        "not display the row if wholesalers source is not present" in new RowFixture {
          val moneySources = MoneySources(Some(BankMoneySource(bankName)), None, Some(true))

          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model.copy(whichCurrencies =
                Some(WhichCurrencies(Seq(currencies.head), Some(UsesForeignCurrenciesYes), Some(moneySources)))
              ),
              businessMatching()
            )
            .rows

          assertRowIsNotPresent("msb.which_currencies.source.which_wholesalers")
        }
      }

      "for the Foreign exchange transactions row" must {

        "display correct row when it is present" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model,
              businessMatching()
            )
            .rows

          assertRowMatches(
            16,
            "renewal.msb.fx.transactions.expected.title",
            fxTransactions,
            controllers.renewal.routes.FXTransactionsInLast12MonthsController.get(edit = true).url,
            "msbfxtransactionsexpected-edit"
          )
        }
      }

      "for the Customers outside UK rows" must {

        "render correctly when answer is Yes and has a single country" in new RowFixture {
          override val summaryListRows: Seq[Aliases.SummaryListRow] = cyaHelper
            .getSummaryList(
              model.copy(customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(countries.head))))),
              businessMatching()
            )
            .rows

          assertRowMatches(
            17,
            "renewal.customer.outside.uk.title",
            booleanToLabel(true),
            controllers.renewal.routes.CustomersOutsideIsUKController.get(true).url,
            "customersoutsideisuk-edit"
          )

          assertRowMatches(
            18,
            "renewal.customer.outside.uk.countries.title",
            countries.head.name,
            controllers.renewal.routes.CustomersOutsideUKController.get(true).url,
            "customersoutsideuk-edit"
          )
        }

        "render correctly when answer is Yes and has a multiple countries" in new RowFixture {
          override val summaryListRows: Seq[Aliases.SummaryListRow] = cyaHelper
            .getSummaryList(
              model,
              businessMatching()
            )
            .rows

          assertRowMatches(
            17,
            "renewal.customer.outside.uk.title",
            booleanToLabel(true),
            controllers.renewal.routes.CustomersOutsideIsUKController.get(true).url,
            "customersoutsideisuk-edit"
          )

          assertRowMatches(
            18,
            "renewal.customer.outside.uk.countries.title",
            toBulletList(countries.map(_.name)),
            controllers.renewal.routes.CustomersOutsideUKController.get(true).url,
            "customersoutsideuk-edit"
          )
        }

        "render correctly when answer is No" in new RowFixture {
          override val summaryListRows: Seq[Aliases.SummaryListRow] = cyaHelper
            .getSummaryList(
              model.copy(
                customersOutsideIsUK = Some(CustomersOutsideIsUK(false)),
                customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(countries.head))))
              ),
              businessMatching()
            )
            .rows

          assertRowMatches(
            17,
            "renewal.customer.outside.uk.title",
            booleanToLabel(false),
            controllers.renewal.routes.CustomersOutsideIsUKController.get(true).url,
            "customersoutsideisuk-edit"
          )

          assertRowIsNotPresent("renewal.customer.outside.uk.countries.title")
        }
      }

      "for the Percentage of cash payments row" must {

        PercentageOfCashPaymentOver15000.all.foreach { percentage =>
          s"display for ${percentage.toString} radio" in new RowFixture {
            override val summaryListRows: Seq[SummaryListRow] = cyaHelper
              .getSummaryList(
                model.copy(percentageOfCashPaymentOver15000 = Some(percentage)),
                businessMatching()
              )
              .rows

            assertRowMatches(
              19,
              "renewal.hvd.percentage.cya",
              messages(s"hvd.percentage.lbl.${percentage.value}"),
              controllers.renewal.routes.PercentageOfCashPaymentOver15000Controller.get(true).url,
              "hvdpercentage-edit"
            )
          }
        }
      }

      "for the Cash payments rows" must {

        "render correctly when answer is Yes and has a single method" in new RowFixture {
          override val summaryListRows: Seq[Aliases.SummaryListRow] = cyaHelper
            .getSummaryList(
              model.copy(receiveCashPayments =
                Some(
                  CashPayments(
                    CashPaymentsCustomerNotMet(true),
                    Some(HowCashPaymentsReceived(PaymentMethods(false, false, Some(otherPaymentMethod))))
                  )
                )
              ),
              businessMatching()
            )
            .rows

          assertRowMatches(
            20,
            "renewal.receiving.title",
            booleanToLabel(true),
            controllers.renewal.routes.CashPaymentsCustomersNotMetController.get(true).url,
            "receivecashpayments-edit"
          )

          assertRowMatches(
            21,
            "renewal.cash.payments.received.title",
            otherPaymentMethod,
            controllers.renewal.routes.HowCashPaymentsReceivedController.get(true).url,
            "receivecashpaymentshowreceived-edit"
          )
        }

        "render correctly when answer is Yes and has a multiple countries" in new RowFixture {
          override val summaryListRows: Seq[Aliases.SummaryListRow] = cyaHelper
            .getSummaryList(
              model,
              businessMatching()
            )
            .rows

          assertRowMatches(
            20,
            "renewal.receiving.title",
            booleanToLabel(true),
            controllers.renewal.routes.CashPaymentsCustomersNotMetController.get(true).url,
            "receivecashpayments-edit"
          )

          assertRowMatches(
            21,
            "renewal.cash.payments.received.title",
            toBulletList(paymentMethods.getSummaryMessages),
            controllers.renewal.routes.HowCashPaymentsReceivedController.get(true).url,
            "receivecashpaymentshowreceived-edit"
          )
        }

        "render correctly when answer is No" in new RowFixture {
          override val summaryListRows: Seq[Aliases.SummaryListRow] = cyaHelper
            .getSummaryList(
              model.copy(receiveCashPayments = Some(CashPayments(CashPaymentsCustomerNotMet(false), None))),
              businessMatching()
            )
            .rows

          assertRowMatches(
            20,
            "renewal.receiving.title",
            booleanToLabel(false),
            controllers.renewal.routes.CashPaymentsCustomersNotMetController.get(true).url,
            "receivecashpayments-edit"
          )
        }
      }

      "not display Transmitting Money Rows" when {

        "Transmitting Money was not selected in MSB services" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model,
              businessMatching(msbActivities = BusinessMatchingMsbServices.all.filterNot(_ == TransmittingMoney).toSet)
            )
            .rows

          assertRowIsNotPresent("renewal.msb.send.money.title")
          assertRowIsNotPresent("renewal.msb.largest.amounts.title")
          assertRowIsNotPresent("renewal.msb.most.transactions.title")
        }
      }

      "not display Currency Exchange Rows" when {

        "Currency Exchange was not selected in MSB services" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model,
              businessMatching(msbActivities = BusinessMatchingMsbServices.all.filterNot(_ == CurrencyExchange).toSet)
            )
            .rows

          assertRowIsNotPresent("renewal.msb.ce.transactions.expected.title")
          assertRowIsNotPresent("renewal.msb.most.transactions.title")
          assertRowIsNotPresent("renewal.msb.foreign_currencies.header")
          assertRowIsNotPresent("renewal.msb.money_sources.header")
          assertRowIsNotPresent("msb.which_currencies.source.which_banks")
          assertRowIsNotPresent("msb.which_currencies.source.which_wholesalers")
        }
      }

      "not display the Foreign Exchange Row" when {

        "Foreign Exchange was not selected in MSB services" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model,
              businessMatching()
            )
            .rows

          assertRowIsNotPresent("renewal.msb.fx.transactions.expected.title")
        }
      }

      "not display the Customers outside UK rows" when {

        "neither Accountancy Services or High Value Dealing are set in Business Matching" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model,
              businessMatching(BusinessActivities.all.filterNot(x => x == AccountancyServices || x == HighValueDealing))
            )
            .rows

          assertRowIsNotPresent("renewal.customer.outside.uk.title")
          assertRowIsNotPresent("renewal.customer.outside.uk.countries.title")
        }
      }
    }
  }
}
