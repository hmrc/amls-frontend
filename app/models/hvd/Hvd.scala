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

package models.hvd

import models.DateOfChange
import models.hvd.Products.{Alcohol, Tobacco}
import models.registrationprogress.{Completed, NotStarted, Section, Started, TaskRow, Updated}
import play.api.Logging
import play.api.i18n.Messages
import play.api.libs.json._
import services.cache.Cache

case class Hvd(
  cashPayment: Option[CashPayment] = None,
  products: Option[Products] = None,
  exciseGoods: Option[ExciseGoods] = None,
  howWillYouSellGoods: Option[HowWillYouSellGoods] = None,
  percentageOfCashPaymentOver15000: Option[PercentageOfCashPaymentOver15000] = None,
  receiveCashPayments: Option[Boolean] = None,
  cashPaymentMethods: Option[PaymentMethods] = None,
  linkedCashPayment: Option[LinkedCashPayments] = None,
  dateOfChange: Option[DateOfChange] = None,
  hasChanged: Boolean = false,
  hasAccepted: Boolean = false
) extends Logging {

  def cashPayment(p: CashPayment): Hvd =
    this.copy(
      cashPayment = Some(p),
      hasChanged = hasChanged || !this.cashPayment.contains(p),
      hasAccepted = hasAccepted && this.cashPayment.contains(p)
    )

  def products(p: Products): Hvd =
    this.copy(
      products = Some(p),
      hasChanged = hasChanged || !this.products.contains(p),
      hasAccepted = hasAccepted && this.products.contains(p)
    )

  def receiveCashPayments(p: Boolean): Hvd =
    this.copy(
      receiveCashPayments = Some(p),
      hasChanged = hasChanged || !this.receiveCashPayments.contains(p),
      hasAccepted = hasAccepted && this.receiveCashPayments.contains(p)
    )

  def cashPaymentMethods(p: PaymentMethods): Hvd =
    this.copy(
      cashPaymentMethods = Some(p),
      hasChanged = hasChanged || !this.cashPaymentMethods.contains(p),
      hasAccepted = hasAccepted && this.cashPaymentMethods.contains(p)
    )

  def exciseGoods(p: ExciseGoods): Hvd =
    this.copy(
      exciseGoods = Some(p),
      hasChanged = hasChanged || !this.exciseGoods.contains(p),
      hasAccepted = hasAccepted && this.exciseGoods.contains(p)
    )

  def linkedCashPayment(p: LinkedCashPayments): Hvd =
    this.copy(
      linkedCashPayment = Some(p),
      hasChanged = hasChanged || !this.linkedCashPayment.contains(p),
      hasAccepted = hasAccepted && this.linkedCashPayment.contains(p)
    )

  def howWillYouSellGoods(p: HowWillYouSellGoods): Hvd =
    copy(
      howWillYouSellGoods = Some(p),
      hasChanged = hasChanged || !this.howWillYouSellGoods.contains(p),
      hasAccepted = hasAccepted && this.howWillYouSellGoods.contains(p)
    )

  def percentageOfCashPaymentOver15000(v: PercentageOfCashPaymentOver15000): Hvd =
    this.copy(
      percentageOfCashPaymentOver15000 = Some(v),
      hasChanged = hasChanged || !this.percentageOfCashPaymentOver15000.contains(v),
      hasAccepted = hasAccepted && this.percentageOfCashPaymentOver15000.contains(v)
    )

  def dateOfChange(v: DateOfChange): Hvd = this.copy(dateOfChange = Some(v))

  def isComplete: Boolean = {
    // $COVERAGE-OFF$
    logger.debug(s"[Hvd][isComplete] $this")
    // $COVERAGE-ON$
    this match {
      case Hvd(Some(cp), Some(pr), _, Some(_), Some(_), Some(true), Some(_), Some(_), _, _, true)
          if pr.items.forall(item => item != Alcohol && item != Tobacco) && cp.isCashPaymentsComplete =>
        true

      case Hvd(Some(cp), Some(_), Some(_), Some(_), Some(_), Some(true), Some(_), Some(_), _, _, true)
          if cp.isCashPaymentsComplete =>
        true

      case Hvd(Some(cp), Some(pr), _, Some(_), Some(_), Some(false), _, Some(_), _, _, true)
          if pr.items.forall(item => item != Alcohol && item != Tobacco) && cp.isCashPaymentsComplete =>
        true

      case Hvd(Some(cp), Some(_), Some(_), Some(_), Some(_), Some(false), _, Some(_), _, _, true)
          if cp.isCashPaymentsComplete =>
        true

      case _ => false
    }
  }
}

object Hvd {

  import utils.MappingUtils._

  val key = "hvd"

  def section(implicit cache: Cache): Section = {
    val notStarted = Section(key, NotStarted, false, controllers.hvd.routes.WhatYouNeedController.get)
    cache.getEntry[Hvd](key).fold(notStarted) { model =>
      if (model.isComplete) {
        Section(key, Completed, model.hasChanged, controllers.hvd.routes.SummaryController.get)
      } else {
        Section(key, Started, model.hasChanged, controllers.hvd.routes.WhatYouNeedController.get)
      }
    }
  }

  def taskRow(implicit cache: Cache, messages: Messages): TaskRow = {
    val notStarted = TaskRow(
      key,
      controllers.hvd.routes.WhatYouNeedController.get.url,
      hasChanged = false,
      NotStarted,
      TaskRow.notStartedTag
    )
    cache.getEntry[Hvd](key).fold(notStarted) { model =>
      if (model.isComplete && model.hasChanged) {
        TaskRow(
          key,
          controllers.hvd.routes.SummaryController.get.url,
          hasChanged = true,
          status = Updated,
          tag = TaskRow.updatedTag
        )
      } else if (model.isComplete) {
        TaskRow(
          key,
          controllers.hvd.routes.SummaryController.get.url,
          model.hasChanged,
          Completed,
          TaskRow.completedTag
        )
      } else {
        TaskRow(
          key,
          controllers.hvd.routes.WhatYouNeedController.get.url,
          model.hasChanged,
          Started,
          TaskRow.incompleteTag
        )
      }
    }
  }

  def oldReceiveCashPaymentsReader: Reads[Option[Boolean]] =
    (__ \ "receiveCashPayments").readNullable[ReceiveCashPayments] map { rcp =>
      rcp map { _.paymentMethods.isDefined }
    }

  def oldCashPaymentMethodsReader: Reads[Option[PaymentMethods]] =
    (__ \ "receiveCashPayments").readNullable[ReceiveCashPayments] map {
      _ flatMap { _.paymentMethods }
    } orElse constant(None)

  def cashPaymentMethodsReader: Reads[Option[PaymentMethods]] =
    (__ \ "cashPaymentMethods").readNullable[PaymentMethods] flatMap {
      case None => oldCashPaymentMethodsReader
      case p    => constant(p)
    }

  implicit val reads: Reads[Hvd] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "cashPayment").readNullable[CashPayment] and
        (__ \ "products").readNullable[Products] and
        (__ \ "exciseGoods").readNullable[ExciseGoods] and
        (__ \ "howWillYouSellGoods").readNullable[HowWillYouSellGoods] and
        (__ \ "percentageOfCashPaymentOver15000").readNullable[PercentageOfCashPaymentOver15000] and
        ((__ \ "receiveCashPayments").readNullable[Boolean] orElse oldReceiveCashPaymentsReader) and
        cashPaymentMethodsReader and
        (__ \ "linkedCashPayment").readNullable[LinkedCashPayments] and
        (__ \ "dateOfChange").readNullable[DateOfChange] and
        (__ \ "hasChanged").readNullable[Boolean].map(_.getOrElse(false)) and
        (__ \ "hasAccepted").readNullable[Boolean].map(_.getOrElse(false))
    ) apply Hvd.apply _
  }

  implicit val writes: Writes[Hvd] = Json.writes[Hvd]

  implicit val formatOption: Reads[Option[Hvd]] = Reads.optionWithNull[Hvd]

  implicit def default(hvd: Option[Hvd]): Hvd =
    hvd.getOrElse(Hvd())
}
