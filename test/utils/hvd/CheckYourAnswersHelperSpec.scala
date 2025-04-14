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

package utils.hvd

import models.hvd.Products.{Alcohol, Other => ProductsOther}
import models.hvd.SalesChannel.Retail
import models.hvd._
import org.scalatest.Assertion
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryListRow
import utils.{AmlsSpec, DateHelper}

import java.time.LocalDate

class CheckYourAnswersHelperSpec extends AmlsSpec {

  lazy val cyaHelper: CheckYourAnswersHelper = app.injector.instanceOf[CheckYourAnswersHelper]

  val otherProduct       = "Bars of soap"
  val otherPaymentMethod = "Vouchers"

  val productsList = Products.all.map { x =>
    if (x.value == ProductsOther("").value) ProductsOther(otherProduct) else x
  }
  val channelsList = SalesChannel.all

  val firstDate = LocalDate.now.minusYears(3)

  val paymentMethods = PaymentMethods(true, true, Some(otherPaymentMethod))

  val model: Hvd = Hvd(
    products = Some(Products(productsList.toSet)),
    exciseGoods = Some(ExciseGoods(true)),
    howWillYouSellGoods = Some(HowWillYouSellGoods(channelsList.toSet)),
    cashPayment = Some(CashPayment(CashPaymentOverTenThousandEuros(true), Some(CashPaymentFirstDate(firstDate)))),
    percentageOfCashPaymentOver15000 = Some(PercentageOfCashPaymentOver15000.First),
    receiveCashPayments = Some(true),
    cashPaymentMethods = Some(paymentMethods),
    linkedCashPayment = Some(LinkedCashPayments(true))
  )

  trait RowFixture {

    val summaryListRows: Seq[SummaryListRow]

    def assertRowMatches(index: Int, title: String, value: String, changeUrl: String, changeId: String): Assertion = {

      val result = summaryListRows.lift(index).getOrElse(fail(s"Row for index $index does not exist"))

      result.key.toString must include(messages(title))

      result.value.toString must include(value)

      checkChangeLink(result, changeUrl, changeId)
    }

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

  ".createSummaryList" when {

    "Products is present" must {

      "render the correct content for a single product" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper
          .getSummaryList(
            model.copy(products = Some(Products(Set(Alcohol))))
          )
          .rows

        assertRowMatches(
          0,
          "hvd.products.cya",
          Alcohol.getMessage,
          controllers.hvd.routes.ProductsController.get(true).url,
          "hvdproducts-edit"
        )
      }

      "render the correct content for multiple products" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model).rows

        assertRowMatches(
          0,
          "hvd.products.cya",
          toBulletList(Products(productsList.toSet).sorted.map(_.getMessage)),
          controllers.hvd.routes.ProductsController.get(true).url,
          "hvdproducts-edit"
        )
      }
    }

    "Excise goods is present" must {

      "render the correct content for 'No'" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper
          .getSummaryList(
            model.copy(exciseGoods = Some(ExciseGoods(false)))
          )
          .rows

        assertRowMatches(
          1,
          "hvd.excise.goods.title",
          booleanToLabel(false),
          controllers.hvd.routes.ExciseGoodsController.get(true).url,
          "hvdexcisegoods-edit"
        )
      }

      "render the correct content for 'Yes'" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model).rows

        assertRowMatches(
          1,
          "hvd.excise.goods.title",
          booleanToLabel(true),
          controllers.hvd.routes.ExciseGoodsController.get(true).url,
          "hvdexcisegoods-edit"
        )
      }
    }

    "How Will You Sell Goods is present" must {

      "render the correct content for a single good" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper
          .getSummaryList(
            model.copy(howWillYouSellGoods = Some(HowWillYouSellGoods(Set(Retail))))
          )
          .rows

        assertRowMatches(
          2,
          "hvd.how-will-you-sell-goods.cya",
          Retail.getMessage,
          controllers.hvd.routes.HowWillYouSellGoodsController.get(true).url,
          "hvdsellgoods-edit"
        )
      }

      "render the correct content for multiple goods" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model).rows

        assertRowMatches(
          2,
          "hvd.how-will-you-sell-goods.cya",
          toBulletList(channelsList.map(_.getMessage)),
          controllers.hvd.routes.HowWillYouSellGoodsController.get(true).url,
          "hvdsellgoods-edit"
        )
      }
    }

    "Cash Payment is present" must {

      "render the correct content for 'No'" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper
          .getSummaryList(
            model.copy(cashPayment = Some(CashPayment(CashPaymentOverTenThousandEuros(false), None)))
          )
          .rows

        assertRowMatches(
          3,
          "hvd.cash.payment.title",
          booleanToLabel(false),
          controllers.hvd.routes.CashPaymentController.get(true).url,
          "hvdcashpayment-edit"
        )
      }

      "render the correct content for 'Yes' including First Date" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model).rows

        assertRowMatches(
          3,
          "hvd.cash.payment.title",
          booleanToLabel(true),
          controllers.hvd.routes.CashPaymentController.get(true).url,
          "hvdcashpayment-edit"
        )

        assertRowMatches(
          4,
          "hvd.cash.payment.date.cya",
          DateHelper.formatDate(firstDate),
          controllers.hvd.routes.CashPaymentFirstDateController.get(true).url,
          "hvdcashpaymentfirstdate-edit"
        )
      }
    }

    "Linked Cash Payment is present" must {

      "render the correct content for 'No'" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper
          .getSummaryList(
            model.copy(linkedCashPayment = Some(LinkedCashPayments(false)))
          )
          .rows

        assertRowMatches(
          5,
          "hvd.identify.linked.cash.payment.title",
          booleanToLabel(false),
          controllers.hvd.routes.LinkedCashPaymentsController.get(true).url,
          "hvdlinkedcashpayments-edit"
        )
      }

      "render the correct content for 'Yes'" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model).rows

        assertRowMatches(
          5,
          "hvd.identify.linked.cash.payment.title",
          booleanToLabel(true),
          controllers.hvd.routes.LinkedCashPaymentsController.get(true).url,
          "hvdlinkedcashpayments-edit"
        )
      }
    }

    "Receive Cash Payment is present" must {

      "render the correct content for 'No'" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper
          .getSummaryList(
            model.copy(receiveCashPayments = Some(false))
          )
          .rows

        assertRowMatches(
          6,
          "hvd.receiving.title",
          booleanToLabel(false),
          controllers.hvd.routes.ReceiveCashPaymentsController.get(true).url,
          "hvdreceivecashpayments-edit"
        )
      }

      "render the correct content for 'Yes'" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model).rows

        assertRowMatches(
          6,
          "hvd.receiving.title",
          booleanToLabel(true),
          controllers.hvd.routes.ReceiveCashPaymentsController.get(true).url,
          "hvdreceivecashpayments-edit"
        )
      }
    }

    "Cash Payment Methods is present" must {

      "render the correct content for a single method" in new RowFixture {

        val singlePaymentMethod = PaymentMethods(true, false, None)

        override val summaryListRows: Seq[SummaryListRow] = cyaHelper
          .getSummaryList(
            model.copy(cashPaymentMethods = Some(singlePaymentMethod))
          )
          .rows

        assertRowMatches(
          7,
          "hvd.receiving.expect.to.receive.cya",
          singlePaymentMethod.getSummaryMessages.mkString,
          controllers.hvd.routes.ExpectToReceiveCashPaymentsController.get(true).url,
          "hvdcashpaymentmethods-edit"
        )
      }

      "render the correct content for multiple methods" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model).rows

        assertRowMatches(
          7,
          "hvd.receiving.expect.to.receive.cya",
          toBulletList(paymentMethods.getSummaryMessages),
          controllers.hvd.routes.ExpectToReceiveCashPaymentsController.get(true).url,
          "hvdcashpaymentmethods-edit"
        )
      }
    }

    "Percentage Payment is present" must {

      PercentageOfCashPaymentOver15000.all.foreach { percentage =>
        s"render the correct content for $percentage." in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = cyaHelper
            .getSummaryList(
              model.copy(percentageOfCashPaymentOver15000 = Some(percentage))
            )
            .rows

          assertRowMatches(
            8,
            "hvd.percentage.cya",
            messages(s"hvd.percentage.lbl.${percentage.value}"),
            controllers.hvd.routes.PercentageOfCashPaymentOver15000Controller.get(true).url,
            "hvdpercentcashpayment-edit"
          )
        }
      }
    }
  }
}
