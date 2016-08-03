package models.tradingpremises

import models.registrationprogress.{Completed, NotStarted, Section, Started}
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap

case class TradingPremises(
                            businessStructure: Option[BusinessStructure] = None,
                            yourTradingPremises: Option[YourTradingPremises] = None,
                            whatDoesYourBusinessDoAtThisAddress : Option[WhatDoesYourBusinessDo] = None,
                            msbServices: Option[MsbServices] = None
                          ) {

  def businessStructure(v: BusinessStructure): TradingPremises =
    this.copy(businessStructure = Some(v))

  def yourTradingPremises(v: YourTradingPremises): TradingPremises =
    this.copy(yourTradingPremises = Some(v))

  def whatDoesYourBusinessDoAtThisAddress(v: WhatDoesYourBusinessDo): TradingPremises =
    this.copy(whatDoesYourBusinessDoAtThisAddress = Some(v))

  def msbServices(v: MsbServices): TradingPremises =
    this.copy(msbServices = Some(v))

  def isComplete: Boolean =
    this match {
      case TradingPremises(_, Some(x),Some(_),_) if x.isOwner => true
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

  implicit val reads: Reads[TradingPremises] = (
      __.read[Option[BusinessStructure]] and
      __.read[Option[YourTradingPremises]] and
      __.read[Option[WhatDoesYourBusinessDo]] and
      __.read[Option[MsbServices]]
    ) (TradingPremises.apply _)

  implicit val writes: Writes[TradingPremises] = Writes[TradingPremises] {
    model =>
      Seq(
        Json.toJson(model.yourTradingPremises).asOpt[JsObject],
        Json.toJson(model.businessStructure).asOpt[JsObject],
        Json.toJson(model.whatDoesYourBusinessDoAtThisAddress).asOpt[JsObject],
        Json.toJson(model.msbServices).asOpt[JsObject]
      ).flatten.fold(Json.obj()) {
        _ ++ _
      }
  }

  implicit def default(tradingPremises: Option[TradingPremises]): TradingPremises =
    tradingPremises.getOrElse(TradingPremises())
}
