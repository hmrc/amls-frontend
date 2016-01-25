package models.tradingpremises

case class TradingPremises(
                            yourTradingPremises: Option[YourTradingPremises] = None,
                            yourAgent: Option[YourAgent] = None
                           ) {

  def yourAgent(v: YourAgent): TradingPremises=
    this.copy(yourAgent = Some(v))


  def yourTradingPremises(v: YourTradingPremises): TradingPremises =
    this.copy(yourTradingPremises = Some(v))
}

object TradingPremises {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  val key = "trading-premises"
  implicit val reads: Reads[TradingPremises] = (
    __.read[Option[YourTradingPremises]] and
      __.read[Option[YourAgent]]
    ) (TradingPremises.apply _)

  implicit val writes: Writes[TradingPremises] = Writes[TradingPremises] {
    model =>
      Seq(
        Json.toJson(model.yourTradingPremises).asOpt[JsObject],
        Json.toJson(model.yourAgent).asOpt[JsObject]

      ).flatten.fold(Json.obj()) {
        _ ++ _
      }
  }

  implicit def default(aboutYou: Option[TradingPremises]): TradingPremises =
    aboutYou.getOrElse(TradingPremises())
}


