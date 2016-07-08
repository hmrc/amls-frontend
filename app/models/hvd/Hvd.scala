package models.hvd


import models.registrationprogress.{Started, Completed, NotStarted, Section}
import play.api.data.mapping.{To, Write}
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap


case class Hvd (cashPayment: Option[CashPayment] = None,
                products: Option[Products] = None,
                exciseGoods:  Option[ExciseGoods] = None,
                linkedCashPayment: Option[LinkedCashPayments] = None,
		howWillYouSellGoods: Option[HowWillYouSellGoods] = None) {


  def cashPayment(v: CashPayment): Hvd =
    this.copy(cashPayment = Some(v))

  def products(v: Products): Hvd =
    this.copy(products = Some(v))

  def exciseGoods(v: ExciseGoods): Hvd =
    this.copy(exciseGoods = Some(v))

  def linkedCashPayment(v: LinkedCashPayments): Hvd =
    this.copy(linkedCashPayment = Some(v))

  def isComplete: Boolean =
    this match {
      case Hvd(Some(_), Some(_), Some(_), Some(_), Some(_)) => true
      case _ => false
    }

  def howWillYouSellGoods(data: HowWillYouSellGoods)  : Hvd = {
    copy(howWillYouSellGoods = Some(data))
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


