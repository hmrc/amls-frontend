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

import forms.EmptyForm
import models.Country
import models.businessmatching._
import models.businessmatching.BusinessActivity._
import models.businessmatching.BusinessMatchingMsbService._
import models.renewal._
import org.jsoup.nodes.Element
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.i18n.Messages
import play.api.test.FakeRequest
import utils.AmlsSummaryViewSpec
import views.Fixture
import views.html.renewal.summary

import scala.collection.JavaConversions._

class summarySpec extends AmlsSummaryViewSpec with TableDrivenPropertyChecks {

  trait ViewFixture extends Fixture {
    lazy val summary = app.injector.instanceOf[summary]
    implicit val requestWithToken = addTokenForView(FakeRequest())
  }

  "summary view" must {
    "have correct title" in new ViewFixture {
      def view = summary(EmptyForm, Renewal(), None, None, true)

      doc.title must startWith(Messages("title.cya") + " - " + Messages("summary.renewal"))
    }

    "have correct headings" in new ViewFixture {
      def view = summary(EmptyForm, Renewal(), None, None, true)

      heading.html must be(Messages("title.cya"))
      subHeading.html must include(Messages("summary.renewal"))
    }

    def checkListContainsItems(parent:Element, keysToFind:Set[String]) = {
      val texts = parent.select("li").toSet.map((el:Element) => el.text())
      texts must be (keysToFind.map(k => Messages(k)))
      true
    }

    val fullActivitiesSet = Set(
      "businessmatching.registerservices.servicename.lbl.01",
      "businessmatching.registerservices.servicename.lbl.03",
      "businessmatching.registerservices.servicename.lbl.04",
      "businessmatching.registerservices.servicename.lbl.05",
      "businessmatching.registerservices.servicename.lbl.06",
      "businessmatching.registerservices.servicename.lbl.07",
      "businessmatching.registerservices.servicename.lbl.08"
    )

    "include the provided data" in new ViewFixture {

      val sectionChecks = Table[String, Element=>Boolean, String](
        ("title key", "check", "edit link"),
        ("renewal.involvedinother.title",checkElementTextIncludes(_, "Yes"), controllers.renewal.routes.InvolvedInOtherController.get(true).toString),
        ("renewal.involvedinother.cya.second.title",checkElementTextIncludes(_, "test text"), controllers.renewal.routes.InvolvedInOtherController.get(true).toString),
        ("renewal.business-turnover.title", checkElementTextIncludes(_, "£0 to £14,999"), controllers.renewal.routes.BusinessTurnoverController.get(true).toString),
        ("renewal.turnover.title", checkElementTextIncludes(_, "£0 to £14,999"), controllers.renewal.routes.AMLSTurnoverController.get(true).toString),
        ("renewal.amp.turnover.title", checkElementTextIncludes(_, "hvd.percentage.lbl.01"), controllers.renewal.routes.AMPTurnoverController.get(true).toString),
        ("renewal.customer.outside.uk.title", checkElementTextIncludes(_, "Yes"), controllers.renewal.routes.CustomersOutsideIsUKController.get(true).toString),
        ("renewal.customer.outside.uk.countries.title", checkElementTextIncludes(_, "United Kingdom"), controllers.renewal.routes.CustomersOutsideUKController.get(true).toString),
        ("renewal.hvd.percentage.title", checkElementTextIncludes(_, "hvd.percentage.lbl.01"), controllers.renewal.routes.PercentageOfCashPaymentOver15000Controller.get(true).toString),
        ("renewal.receiving.title", checkElementTextIncludes(_, "Yes"), controllers.renewal.routes.CashPaymentsCustomersNotMetController.get(true).toString),
        ("renewal.cash.payments.received.title", checkElementTextIncludes(_, "Directly into a bank account"), controllers.renewal.routes.HowCashPaymentsReceivedController.get(true).toString),
        ("renewal.msb.throughput.header", checkElementTextIncludes(_, "renewal.msb.throughput.selection.1"), controllers.renewal.routes.TotalThroughputController.get(true).toString),
        ("renewal.msb.transfers.header", checkElementTextIncludes(_, "1500"), controllers.renewal.routes.TransactionsInLast12MonthsController.get(true).toString),
        ("renewal.msb.largest.amounts.title", checkElementTextIncludes(_, "France"), controllers.renewal.routes.SendTheLargestAmountsOfMoneyController.get(true).toString),
        ("renewal.msb.most.transactions.title", checkElementTextIncludes(_, "United Kingdom"), controllers.renewal.routes.MostTransactionsController.get(true).toString),
        ("renewal.msb.whichcurrencies.header", checkElementTextIncludes(_, "EUR"), controllers.renewal.routes.WhichCurrenciesController.get(true).toString),
        ("msb.which_currencies.source.which_banks", checkElementTextIncludes(_, "testBank"), controllers.renewal.routes.MoneySourcesController.get(true).toString),
        ("msb.which_currencies.source.which_wholesalers", checkElementTextIncludes(_, "testWholeSaler"), controllers.renewal.routes.MoneySourcesController.get(true).toString),
        ("renewal.msb.ce.transactions.expected.title", checkElementTextIncludes(_, "123"), controllers.renewal.routes.CETransactionsInLast12MonthsController.get(true).toString),
        ("renewal.msb.fx.transactions.expected.title", checkElementTextIncludes(_, "12"), controllers.renewal.routes.FXTransactionsInLast12MonthsController.get(true).toString)
      )

      def view = {
        val renewalModel = Renewal(
          Some(InvolvedInOtherYes("test text")),
          Some(BusinessTurnover.First),
          Some(AMLSTurnover.First),
          Some(AMPTurnover.First),
          Some(CustomersOutsideIsUK(true)),
          Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
          Some(PercentageOfCashPaymentOver15000.First),
          Some(CashPayments(CashPaymentsCustomerNotMet(true), Some(HowCashPaymentsReceived(PaymentMethods(true,true,Some("other")))))),
          Some(TotalThroughput("01")),
          Some(WhichCurrencies(Seq("EUR"),None,Some(MoneySources(Some(BankMoneySource("testBank")),Some(WholesalerMoneySource("testWholeSaler")),None)))),
          Some(TransactionsInLast12Months("1500")),
          Some(SendTheLargestAmountsOfMoney(Seq(Country("France", "FR")))),
          Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
          Some(CETransactionsInLast12Months("123")),
          Some(FXTransactionsInLast12Months("12")),
          false
        )

        val businessActivitiesModel = BusinessActivities(
          Set(AccountancyServices,
            BillPaymentServices,
            EstateAgentBusinessService,
            HighValueDealing,
            MoneyServiceBusiness,
            TrustAndCompanyServices,
            TelephonePaymentService)
        )

        val msbServices = Some(
          BusinessMatchingMsbServices(
            Set(
              TransmittingMoney,
              CurrencyExchange,
              ChequeCashingNotScrapMetal,
              ChequeCashingScrapMetal,
              ForeignExchange
            )
          )
        )
        val businessTypesList = Some(
          List(
            "Accountancy service provider",
            "Estate agency business",
            "Telecommunications, digital and IT payment service provider",
            "Bill payment service provider",
            "Trust or company service provider",
            "High value dealer",
            "Money service business"
          ))
        summary(EmptyForm, renewalModel, businessTypesList, msbServices, true)
      }

      forAll(sectionChecks) { (key, check, editLink) => {
        val headers = doc.select("span.bold")
        val header = headers.toList.find(e => e.text() == Messages(key))
        header must not be None
        val section = header.get.parents().select("div").first()
        check(section) must be(true)
        section.select("a[href]").attr("href") must include(editLink)
      }}
    }
  }
}
