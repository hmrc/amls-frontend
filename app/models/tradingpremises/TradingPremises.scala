package models.tradingpremises

import models.registrationprogress.{Completed, NotStarted, Section, Started}
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap

case class TradingPremises(
                            yourTradingPremises: Option[YourTradingPremises] = None,
                            yourAgent: Option[YourAgent] = None,
                            whatDoesYourBusinessDoAtThisAddress : Option[WhatDoesYourBusinessDo] = None,
                            msbServices: Option[MsbServices] = None
                          ) {

  def yourAgent(v: YourAgent): TradingPremises =
    this.copy(yourAgent = Some(v))

  def yourTradingPremises(v: YourTradingPremises): TradingPremises =
    this.copy(yourTradingPremises = Some(v))

  def whatDoesYourBusinessDoAtThisAddress(v: WhatDoesYourBusinessDo): TradingPremises =
    this.copy(whatDoesYourBusinessDoAtThisAddress = Some(v))

  def msbServices(v: MsbServices): TradingPremises =
    this.copy(msbServices = Some(v))

  def isComplete: Boolean =
    this match {
      case TradingPremises(Some(x), _, Some(_),_) if x.isOwner => true
      case TradingPremises(Some(_), Some(_), Some(_), _) => true
      case _ => false
    }
}

object TradingPremises {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "tradingpremises"
    val notStarted = Section(messageKey, NotStarted, controllers.tradingpremises.routes.WhatYouNeedController.get(1))
    cache.getEntry[Seq[TradingPremises]](key).fold(notStarted) {
      premises =>
        if (premises.nonEmpty && (premises forall { _.isComplete })) {
          Section(messageKey, Completed, controllers.tradingpremises.routes.SummaryController.get())
        } else {
          val index = premises.indexWhere {
            case model if !model.isComplete => true
            case _ => false
          }
          Section(messageKey, Started, controllers.tradingpremises.routes.WhatYouNeedController.get(index + 1))
        }
    }
  }

  val key = "trading-premises"

  implicit val mongoKey = new MongoKey[TradingPremises] {
    override def apply(): String = "trading-premises"
  }

  implicit val format = Json.format[TradingPremises]

  implicit def default(tradingPremises: Option[TradingPremises]): TradingPremises =
    tradingPremises.getOrElse(TradingPremises())
}
