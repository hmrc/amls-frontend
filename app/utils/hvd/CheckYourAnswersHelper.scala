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

import models.hvd.Products.{Alcohol, Tobacco}
import models.hvd.{HowWillYouSellGoods, Hvd, PaymentMethods, Products}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{SummaryList, SummaryListRow, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Key
import utils.{CheckYourAnswersHelperFunctions, DateHelper}

import javax.inject.Inject

class CheckYourAnswersHelper @Inject()() extends CheckYourAnswersHelperFunctions {

  def getSummaryList(model: Hvd)(implicit messages: Messages): SummaryList = {

    SummaryList(
      Seq(
        productRow(model),
        exciseGoodsRow(model),
        howWillYouSellGoodsRow(model)
      ).flatten ++ cashPaymentRows(model).getOrElse(Seq.empty[SummaryListRow]) ++
      Seq(
        linkedCashPaymentRow(model),
        receiveCashPaymentsRow(model),
        cashPaymentMethodsRow(model),
        percentagePaymentsRow(model)
      ).flatten
    )
  }

  private def productRow(model: Hvd)(implicit messages: Messages): Option[SummaryListRow] = {

    def productsValue(products: Products): Value = {
      products.sorted.toList match {
        case item :: Nil => Value(Text(item.getMessage))
        case items => toBulletList(items.map(_.getMessage))
      }
    }

    model.products.map { products =>
      SummaryListRow(
        Key(Text(messages("hvd.products.title"))),
        productsValue(products),
        actions = editAction(
          controllers.hvd.routes.ProductsController.get(true).url,
          "hvd.checkYourAnswers.change.products",
          "hvdproducts-edit"
        )
      )
    }
  }

  private def exciseGoodsRow(model: Hvd)(implicit messages: Messages): Option[SummaryListRow] = {

    for {
      products <- model.products
      exciseGoods <- model.exciseGoods
    } yield {
      if(products.items.contains(Alcohol) || products.items.contains(Tobacco)) {
        Some(
          row(
            "hvd.excise.goods.title",
            booleanToLabel(exciseGoods.exciseGoods),
            editAction(
              controllers.hvd.routes.ExciseGoodsController.get(true).url,
              "hvd.checkYourAnswers.change.duty-suspendedProducts",
              "hvdexcisegoods-edit"
            )
          )
        )
      } else {
        None
      }
    }
  }.flatten

  private def howWillYouSellGoodsRow(model: Hvd)(implicit messages: Messages): Option[SummaryListRow] = {

    def goodsValue(goods: HowWillYouSellGoods): Value = {
      goods.channels.toList match {
        case item :: Nil => Value(Text(item.getMessage))
        case items => toBulletList(items.map(_.getMessage))
      }
    }

    model.howWillYouSellGoods.map { goods =>

      SummaryListRow(
        Key(Text(messages("hvd.how-will-you-sell-goods.title"))),
        goodsValue(goods),
        actions = editAction(
          controllers.hvd.routes.HowWillYouSellGoodsController.get(true).url,
          "hvd.checkYourAnswers.change.howSold",
          "hvdsellgoods-edit"
        )
      )
    }
  }

  private def cashPaymentRows(model: Hvd)(implicit messages: Messages): Option[Seq[SummaryListRow]] = {

    model.cashPayment.map { payment =>
      Seq(
        Some(
          row(
            "hvd.cash.payment.title",
            booleanToLabel(payment.acceptedPayment.acceptedAnyPayment),
            editAction(
              controllers.hvd.routes.CashPaymentController.get(true).url,
              "hvd.checkYourAnswers.change.cashPayments",
              "hvdcashpayment-edit"
            )
          )
        ),
        payment.firstDate.map { date =>
          row(
            "hvd.cash.payment.date.title",
            DateHelper.formatDate(date.paymentDate),
            editAction(
              controllers.hvd.routes.CashPaymentFirstDateController.get(true).url,
              "hvd.checkYourAnswers.change.firstPayDate",
              "hvdcashpaymentfirstdate-edit"
            )
          )
        }
      ).flatten
    }
  }

  private def linkedCashPaymentRow(model: Hvd)(implicit messages: Messages): Option[SummaryListRow] = {

    model.linkedCashPayment.map { payments =>
      row(
        "hvd.identify.linked.cash.payment.title",
        booleanToLabel(payments.linkedCashPayments),
        editAction(
          controllers.hvd.routes.LinkedCashPaymentsController.get(true).url,
          "hvd.checkYourAnswers.change.autoIdentifyLinkdPay",
          "hvdlinkedcashpayments-edit"
        )
      )
    }
  }

  private def receiveCashPaymentsRow(model: Hvd)(implicit messages: Messages): Option[SummaryListRow] = {

    model.receiveCashPayments.map { payments =>
      row(
        "hvd.receiving.title",
        booleanToLabel(payments),
        editAction(
          controllers.hvd.routes.ReceiveCashPaymentsController.get(true).url,
          "hvd.checkYourAnswers.change.receiveHighPay",
          "hvdreceivecashpayments-edit"
        )
      )
    }
  }

  private def cashPaymentMethodsRow(model: Hvd)(implicit messages: Messages) = {

    def paymentsValue(methods: PaymentMethods): Value = {
      methods.getSummaryMessages.toList match {
        case message :: Nil => Value(Text(message))
        case msgs => toBulletList(msgs)
      }
    }

    model.cashPaymentMethods.map { cpm =>

      SummaryListRow(
        Key(Text(messages("hvd.receiving.expect.to.receive"))),
        paymentsValue(cpm),
        actions = editAction(
          controllers.hvd.routes.ExpectToReceiveCashPaymentsController.get(true).url,
          "hvd.checkYourAnswers.change.howMoneySent",
          "hvdcashpaymentmethods-edit"
        )
      )
    }
  }

  private def percentagePaymentsRow(model: Hvd)(implicit messages: Messages): Option[SummaryListRow] = {

    model.percentageOfCashPaymentOver15000.map { percentage =>
      row(
        "hvd.percentage.title",
        messages(s"hvd.percentage.lbl.${percentage.value}"),
        editAction(
          controllers.hvd.routes.PercentageOfCashPaymentOver15000Controller.get(true).url,
          "hvd.checkYourAnswers.change.whatPercent",
          "hvdpercentcashpayment-edit"
        )
      )
    }
  }
}
