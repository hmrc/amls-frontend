package models.tradingpremises

import models.registrationprogress.{Completed, NotStarted, Section, Started}
import models.tradingpremises.AgentCompanyName
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap

case class TradingPremises(
                            registeringAgentPremises: Option[RegisteringAgentPremises] = None,
                            yourTradingPremises: Option[YourTradingPremises] = None,
                            yourAgent: Option[YourAgent] = None,
                            agentName: Option[AgentName] = None,
                            agentCompanyName: Option[AgentCompanyName] = None,
                            agentPartnership: Option[AgentPartnership] = None,
                            whatDoesYourBusinessDoAtThisAddress : Option[WhatDoesYourBusinessDo] = None,
                            msbServices: Option[MsbServices] = None

                          ) {

  def yourAgent(v: YourAgent): TradingPremises =
    this.copy(yourAgent = Some(v))

  def agentName(v: AgentName): TradingPremises =
    this.copy(agentName = Some(v))

  def agentCompanyName(v: AgentCompanyName): TradingPremises =
    this.copy(agentCompanyName = Some(v))


  def agentPartnership(v: AgentPartnership): TradingPremises =
    this.copy(agentPartnership = Some(v))


  def yourTradingPremises(v: YourTradingPremises): TradingPremises =
    this.copy(yourTradingPremises = Some(v))

  def whatDoesYourBusinessDoAtThisAddress(v: WhatDoesYourBusinessDo): TradingPremises =
    this.copy(whatDoesYourBusinessDoAtThisAddress = Some(v))

  def msbServices(v: MsbServices): TradingPremises =
    this.copy(msbServices = Some(v))

  def yourAgentPremises(v: RegisteringAgentPremises): TradingPremises =
    this.copy(registeringAgentPremises = Some(v))

  def isComplete: Boolean =
    this match {
      case TradingPremises(_,Some(x), _, _,_,_,Some(_),_) if x.isOwner => true
      case TradingPremises(_,_,Some(_), Some(_),Some(_),Some(_), Some(_), _) => true
      case _ => false
    }
}

object TradingPremises {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "tradingpremises"
    val notStarted = Section(messageKey, NotStarted, controllers.tradingpremises.routes.TradingPremisesAddController.get(true))
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
      __.read[Option[RegisteringAgentPremises]] and
      __.read[Option[YourTradingPremises]] and
      __.read[Option[YourAgent]] and
        __.read[Option[AgentName]] and
        __.read[Option[AgentCompanyName]] and
        __.read[Option[AgentPartnership]] and
      __.read[Option[WhatDoesYourBusinessDo]] and
      __.read[Option[MsbServices]]
    ) (TradingPremises.apply _)

  implicit val writes: Writes[TradingPremises] = Writes[TradingPremises] {
    model =>
      Seq(
        Json.toJson(model.registeringAgentPremises).asOpt[JsObject],
        Json.toJson(model.yourTradingPremises).asOpt[JsObject],
        Json.toJson(model.yourAgent).asOpt[JsObject],
        Json.toJson(model.agentName).asOpt[JsObject],
        Json.toJson(model.agentCompanyName).asOpt[JsObject],
        Json.toJson(model.agentPartnership).asOpt[JsObject],
        Json.toJson(model.whatDoesYourBusinessDoAtThisAddress).asOpt[JsObject],
        Json.toJson(model.msbServices).asOpt[JsObject]
      ).flatten.fold(Json.obj()) {
        _ ++ _
      }
  }

  implicit def default(tradingPremises: Option[TradingPremises]): TradingPremises =
    tradingPremises.getOrElse(TradingPremises())
}
