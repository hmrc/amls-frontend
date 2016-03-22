package models.tradingpremises

import models.registrationprogress.{IsComplete, Section}
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap

case class TradingPremises(
                            yourTradingPremises: Option[YourTradingPremises] = None,
                            yourAgent: Option[YourAgent] = None,
                            whatDoesYourBusinessDoAtThisAddress : Option[WhatDoesYourBusinessDo] = None
                          ) {

  def yourAgent(v: YourAgent): TradingPremises =
    this.copy(yourAgent = Some(v))

  def yourTradingPremises(v: YourTradingPremises): TradingPremises =
    this.copy(yourTradingPremises = Some(v))

  def whatDoesYourBusinessDoAtThisAddress(v: WhatDoesYourBusinessDo): TradingPremises =
    this.copy(whatDoesYourBusinessDoAtThisAddress = Some(v))
}

object TradingPremises {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "tradingpremises"
    val incomplete = Section(messageKey, false, controllers.tradingpremises.routes.WhatYouNeedController.get())
    cache.getEntry[Seq[TradingPremises]](key).fold(incomplete) {
      premises =>
        if (premises.nonEmpty) {
          Section(messageKey, true, controllers.tradingpremises.routes.SummaryController.get())
        } else {
          incomplete
        }
    }
  }

  val key = "trading-premises"

  implicit val mongoKey = new MongoKey[TradingPremises] {
    override def apply(): String = "trading-premises"
  }

  implicit val reads: Reads[TradingPremises] = (
      __.read[Option[YourTradingPremises]] and
      __.read[Option[YourAgent]] and
      __.read[Option[WhatDoesYourBusinessDo]]
    ) (TradingPremises.apply _)

  implicit val writes: Writes[TradingPremises] = Writes[TradingPremises] {
    model =>
      Seq(
        Json.toJson(model.yourTradingPremises).asOpt[JsObject],
        Json.toJson(model.yourAgent).asOpt[JsObject],
        Json.toJson(model.whatDoesYourBusinessDoAtThisAddress).asOpt[JsObject]
      ).flatten.fold(Json.obj()) {
        _ ++ _
      }
  }

  implicit def default(tradingPremises: Option[TradingPremises]): TradingPremises =
    tradingPremises.getOrElse(TradingPremises())
}
