package models.tradingpremises

import models.registrationprogress.{Completed, NotStarted, Section, Started}
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap

case class TradingPremises(
                            registeringAgentPremises: Option[RegisteringAgentPremises] = None,
                            yourTradingPremises: Option[YourTradingPremises] = None,
                            businessStructure: Option[BusinessStructure] = None,
                            agentName: Option[AgentName] = None,
                            agentCompanyName: Option[AgentCompanyName] = None,
                            agentPartnership: Option[AgentPartnership] = None,
                            whatDoesYourBusinessDoAtThisAddress : Option[WhatDoesYourBusinessDo] = None,
                            msbServices: Option[MsbServices] = None

                          ) {

  def businessStructure(v: BusinessStructure): TradingPremises =
    this.copy(businessStructure = Some(v))

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
      case TradingPremises(_,Some(x), _, _,_,_,Some(_),_) => true
      case TradingPremises(_,_,Some(_), Some(_),Some(_),Some(_), Some(_), _) => true
      case TradingPremises(None, None, None, None, None, None, None, None) => true //This code part of fix for the issue AMLS-1549 back button issue
      case _ => false
    }
}

object TradingPremises {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  val key = "trading-premises"

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "tradingpremises"
    val notStarted = Section(messageKey, NotStarted, false, controllers.tradingpremises.routes.TradingPremisesAddController.get(true))
    val complete = Section(messageKey, Completed, false, controllers.tradingpremises.routes.SummaryController.answers())

    cache.getEntry[Seq[TradingPremises]](key).fold(notStarted) {
      _.filterNot(_ == TradingPremises()) match {
        case Nil => notStarted
        case premises if premises.nonEmpty && premises.forall {
          _.isComplete
        } => complete
        case premises => {
          val index = premises.indexWhere {
            case model if !model.isComplete => true
            case _ => false
          }
          Section(messageKey, Started, false, controllers.tradingpremises.routes.WhatYouNeedController.get(index + 1))
        }
      }
    }
  }


  implicit val mongoKey = new MongoKey[TradingPremises] {
    override def apply(): String = "trading-premises"
  }

  implicit val reads: Reads[TradingPremises] = (
      __.read[Option[RegisteringAgentPremises]] and
      __.read[Option[YourTradingPremises]] and
        __.read[Option[BusinessStructure]] and
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
        Json.toJson(model.businessStructure).asOpt[JsObject],
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
