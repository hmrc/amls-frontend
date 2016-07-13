package models.hvd


import models.registrationprogress.{Started, Completed, NotStarted, Section}
import play.api.data.mapping.{To, Write}
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap


case class Hvd (cashPayment: Option[CashPayment] = None,
                products: Option[Products] = None,
                exciseGoods:  Option[ExciseGoods] = None,
		            howWillYouSellGoods: Option[HowWillYouSellGoods] = None,
                percentageOfCashPaymentOver15000: Option[PercentageOfCashPaymentOver15000] = None) {
                receiveCashPayments: Option[ReceiveCashPayments] = None,
                linkedCashPayment: Option[LinkedCashPayments] = None) {

  def cashPayment(v: CashPayment): Hvd =
    this.copy(cashPayment = Some(v))

  def products(v: Products): Hvd =
    this.copy(products = Some(v))

  def receiveCashPayments(v: ReceiveCashPayments): Hvd =
    this.copy(receiveCashPayments = Some(v))

  def exciseGoods(v: ExciseGoods): Hvd =
    this.copy(exciseGoods = Some(v))

  def linkedCashPayment(v: LinkedCashPayments): Hvd =
    this.copy(linkedCashPayment = Some(v))

  def howWillYouSellGoods(data: HowWillYouSellGoods)  : Hvd = {
    copy(howWillYouSellGoods = Some(data))
  }

  def percentageOfCashPaymentOver15000(v: PercentageOfCashPaymentOver15000): Hvd =
    this.copy(percentageOfCashPaymentOver15000 = Some(v))

  def isComplete: Boolean =
    this.productIterator.forall {
      case Some(_) => true
      case _ => false
    }
}

object Hvd {

  val key = "hvd"

  def section(implicit cache: CacheMap): Section = {
    val notStarted = Section(key, NotStarted, controllers.hvd.routes.WhatYouNeedController.get())
    cache.getEntry[Hvd](key).fold(notStarted) {
      case model if model.isComplete =>
        Section(key, Completed, controllers.hvd.routes.SummaryController.get())
      case _ =>
        Section(key, Started, controllers.hvd.routes.WhatYouNeedController.get())
    }
  }

  implicit val format = Json.format[Hvd]

  implicit def default(hvd: Option[Hvd]): Hvd =
    hvd.getOrElse(Hvd())
}


