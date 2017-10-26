/*
 * Copyright 2017 HM Revenue & Customs
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
import org.scalatest.MustMatchers
import utils.GenericTestHelper
import play.api.i18n.Messages
import views.{Fixture, HtmlAssertions}

import scala.collection.JavaConversions._


class summarySpec extends GenericTestHelper
  with MustMatchers
  with TableDrivenPropertyChecks
  with HtmlAssertions{

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "summary view" must {
    "have correct title" in new ViewFixture {
      def view = views.html.hvd.summary(EmptyForm, Hvd(), true)

      doc.title must startWith(Messages("title.cya") + " - " + Messages("summary.hvd"))
    }

    "have correct headings" in new ViewFixture {
      def view = views.html.hvd.summary(EmptyForm, Hvd(), true)

      heading.html must be(Messages("title.cya"))
      subHeading.html must include(Messages("summary.hvd"))
    }

    def checkListContainsItems(parent:Element, keysToFind:Set[String]) = {
      val texts = parent.select("li").toSet.map((el:Element) => el.text())
      texts must be (keysToFind.map(k => Messages(k)))
      true
    }

    val fullProductSet = Set("hvd.products.option.01","hvd.products.option.02","hvd.products.option.03",
      "hvd.products.option.04","hvd.products.option.05","hvd.products.option.06","hvd.products.option.07",
      "hvd.products.option.08","hvd.products.option.09","hvd.products.option.10","hvd.products.option.11",
      "Other Product"
    )

    val sectionChecks = Table[String, Element=>Boolean](
      ("title key", "check"),
      ("hvd.cash.payment.title",checkElementTextIncludes(_, "lbl.yes", "20 June 2012")),
      ("hvd.products.title", checkListContainsItems(_, fullProductSet)),
      ("hvd.excise.goods.title", checkElementTextIncludes(_, "lbl.yes")),
      ("hvd.how-will-you-sell-goods.title", checkListContainsItems(_, Set("Retail", "Auction", "Wholesale"))),
      ("hvd.percentage.title", checkElementTextIncludes(_, "hvd.percentage.lbl.03")),
      ("hvd.receiving.title", checkElementTextIncludes(_, "lbl.yes")),
      ("hvd.receiving.expect.to.receive", checkElementTextIncludes(_, "hvd.receiving.option.01", "hvd.receiving.option.02", "Other payment method")),
      ("hvd.identify.linked.cash.payment.title", checkElementTextIncludes(_, "lbl.yes"))
    )

    "include the provided data" in new ViewFixture {
      def view = {
        val testdata = Hvd(
          cashPayment = Some(CashPaymentYes(LocalDate.parse("2012-6-20"))),
          products = Some(Products(Set(Alcohol,Tobacco,Antiques,Cars,OtherMotorVehicles,
                          Caravans,Jewellery,Gold,ScrapMetals,MobilePhones,Clothing,
                          Other("Other Product")
                        ))),
          exciseGoods = Some(ExciseGoods(true)),
          howWillYouSellGoods = Some(HowWillYouSellGoods(List(Retail, Wholesale, Auction))),
          percentageOfCashPaymentOver15000 = Some(Third),
          receiveCashPayments = Some(true),
          cashPaymentMethods = Some(PaymentMethods(true, true, Some("Other payment method"))),
          linkedCashPayment = Some(LinkedCashPayments(true))
        )

        views.html.hvd.summary(EmptyForm, testdata, true)
      }

      forAll(sectionChecks) { (key, check) => {
        val hTwos = doc.select("section.check-your-answers h2")
        val hTwo = hTwos.toList.find(e => e.text() == Messages(key))

        hTwo must not be None
        val section = hTwo.get.parents().select("section").first()
        check(section) must be(true)
      }}
    }
  }
}
