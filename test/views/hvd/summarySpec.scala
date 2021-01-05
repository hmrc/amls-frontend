/*
 * Copyright 2021 HM Revenue & Customs
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

package views.hvd

import forms.EmptyForm
import models.hvd.PercentageOfCashPaymentOver15000.Third
import models.hvd._
import org.joda.time.LocalDate
import org.jsoup.nodes.Element
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.i18n.Messages
import play.api.test.FakeRequest
import utils.AmlsSummaryViewSpec
import views.Fixture
import views.html.hvd.summary

import scala.collection.JavaConversions._

class summarySpec extends AmlsSummaryViewSpec with TableDrivenPropertyChecks {

  trait ViewFixture extends Fixture {
    lazy val summary = app.injector.instanceOf[summary]
    implicit val requestWithToken = addTokenForView(FakeRequest())
  }

  "summary view" must {
    "have correct title" in new ViewFixture {
      def view = summary(EmptyForm, Hvd())

      doc.title must startWith(Messages("title.cya") + " - " + Messages("summary.hvd"))
    }

    "have correct headings" in new ViewFixture {
      def view = summary(EmptyForm, Hvd())

      heading.html must be(Messages("title.cya"))
      subHeading.html must include(Messages("summary.hvd"))
    }

    def checkListContainsItems(parent:Element, keysToFind:Set[String]) = {
      val texts = parent.select("li").toSet.map((el:Element) => el.text())
      texts must be (keysToFind.map(k => Messages(k)))
      true
    }

    val fullProductSet = Set("hvd.products.option.01","hvd.products.option.03","hvd.products.option.06",
      "hvd.products.option.04","hvd.products.option.11","hvd.products.option.08","hvd.products.option.07",
      "hvd.products.option.10","hvd.products.option.05","hvd.products.option.09","hvd.products.option.02",
      "Other Product"
    )

    "include the provided data" in new ViewFixture {

    val sectionChecks = Table[String, Element=>Boolean](
      ("title key", "check"),
      ("hvd.cash.payment.title",checkElementTextOnlyIncludes(_, "lbl.yes")),
      ("hvd.cash.payment.date.title",checkElementTextOnlyIncludes(_, "20 June 2012")),
      ("hvd.products.title", checkListContainsItems(_, fullProductSet)),
      ("hvd.excise.goods.title", checkElementTextOnlyIncludes(_, "lbl.yes")),
      ("hvd.how-will-you-sell-goods.title", checkListContainsItems(_, Set("Retail", "Auction", "Wholesale"))),
      ("hvd.percentage.title", checkElementTextOnlyIncludes(_, "hvd.percentage.lbl.03")),
      ("hvd.receiving.title", checkElementTextOnlyIncludes(_, "lbl.yes")),
      ("hvd.receiving.expect.to.receive", checkElementTextOnlyIncludes(_, "hvd.receiving.option.01", "hvd.receiving.option.02", "Other payment method")),
      ("hvd.identify.linked.cash.payment.title", checkElementTextOnlyIncludes(_, "lbl.yes"))
    )

      def view = {
        val testdata = Hvd(
          cashPayment = Some(CashPayment(
            CashPaymentOverTenThousandEuros(true),
            Some(CashPaymentFirstDate(LocalDate.parse("2012-6-20"))))),
          products = Some(Products(Set(Alcohol,Tobacco,Antiques,Cars,OtherMotorVehicles,
                          Caravans,Jewellery,Gold,ScrapMetals,MobilePhones,Clothing,
                          Other("Other Product")
                        ))),
          exciseGoods = Some(ExciseGoods(true)),
          howWillYouSellGoods = Some(HowWillYouSellGoods(Set(Retail, Wholesale, Auction))),
          percentageOfCashPaymentOver15000 = Some(Third),
          receiveCashPayments = Some(true),
          cashPaymentMethods = Some(PaymentMethods(true, true, Some("Other payment method"))),
          linkedCashPayment = Some(LinkedCashPayments(true))
        )

        summary(EmptyForm, testdata)
      }

      forAll(sectionChecks) { (key, check) => {
        val hTwos = doc.select(".cya-summary-list__key")
        val hTwo = hTwos.toList.find(e => e.text() == Messages(key))

        hTwo must not be None
        val section = hTwo.get.parent().select(".cya-summary-list__value").first()
        check(section) must be(true)
      }}
    }
  }
}
