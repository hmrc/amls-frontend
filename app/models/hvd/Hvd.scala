package models.hvd


import models.DateOfChange
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import play.Logger
import play.api.libs.json.{Json, Reads, Writes}
import uk.gov.hmrc.http.cache.client.CacheMap


case class Hvd (cashPayment: Option[CashPayment] = None,
                products: Option[Products] = None,
                exciseGoods:  Option[ExciseGoods] = None,
                howWillYouSellGoods: Option[HowWillYouSellGoods] = None,
                percentageOfCashPaymentOver15000: Option[PercentageOfCashPaymentOver15000] = None,
                receiveCashPayments: Option[ReceiveCashPayments] = None,
                linkedCashPayment: Option[LinkedCashPayments] = None,
                dateOfChange: Option[DateOfChange] = None,
                hasChanged: Boolean = false) {

  def cashPayment(p: CashPayment): Hvd =
    this.copy(cashPayment = Some(p), hasChanged = hasChanged || !this.cashPayment.contains(p))

  def products(p: Products): Hvd =
    this.copy(products = Some(p), hasChanged = hasChanged || !this.products.contains(p))

  def receiveCashPayments(p: ReceiveCashPayments): Hvd =
    this.copy(receiveCashPayments = Some(p), hasChanged = hasChanged || !this.receiveCashPayments.contains(p))

  def exciseGoods(p: ExciseGoods): Hvd =
    this.copy(exciseGoods = Some(p), hasChanged = hasChanged || !this.exciseGoods.contains(p))

  def linkedCashPayment(p: LinkedCashPayments): Hvd =
    this.copy(linkedCashPayment = Some(p), hasChanged = hasChanged || !this.linkedCashPayment.contains(p))

  def howWillYouSellGoods(p: HowWillYouSellGoods)  : Hvd = {
    copy(howWillYouSellGoods = Some(p), hasChanged = hasChanged || !this.howWillYouSellGoods.contains(p))
  }

  def percentageOfCashPaymentOver15000(v: PercentageOfCashPaymentOver15000): Hvd =
    this.copy(percentageOfCashPaymentOver15000 = Some(v))

  def dateOfChange(v: DateOfChange): Hvd = this.copy(dateOfChange = Some(v))

  def isComplete: Boolean = {
    Logger.debug(s"[Hvd][isComplete] $this")
    this match {
      case Hvd(Some(_), Some(pr), _, Some(_), Some(_), Some(_), Some(_),_, _)
        if pr.items.forall(item => item != Alcohol && item != Tobacco) => true
      case Hvd(Some(_), Some(pr), Some(_), Some(_), Some(_), Some(_), Some(_), _,_) => true
      case _ => false
    }
  }
}

object Hvd {

  val key = "hvd"

  def section(implicit cache: CacheMap): Section = {
    val notStarted = Section(key, NotStarted, false, controllers.hvd.routes.WhatYouNeedController.get())
    cache.getEntry[Hvd](key).fold(notStarted)  {
      model =>
        if (model.isComplete) {
          Section(key, Completed, model.hasChanged, controllers.hvd.routes.SummaryController.get())
        } else {
          Section(key, Started, model.hasChanged, controllers.hvd.routes.WhatYouNeedController.get())
        }
    }
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
        (__ \ "receiveCashPayments").readNullable[ReceiveCashPayments] and
        (__ \ "linkedCashPayment").readNullable[LinkedCashPayments] and
        (__ \ "dateOfChange").readNullable[DateOfChange] and
        (__ \ "hasChanged").readNullable[Boolean].map {_.getOrElse(false)}
      ) apply Hvd.apply _
  }

  implicit val writes: Writes[Hvd] = Json.writes[Hvd]

  implicit def default(hvd: Option[Hvd]): Hvd =
    hvd.getOrElse(Hvd())
}


