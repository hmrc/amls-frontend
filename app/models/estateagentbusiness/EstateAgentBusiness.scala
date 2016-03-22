package models.estateagentbusiness

import models.registrationprogress.{IsComplete, Section}
import uk.gov.hmrc.http.cache.client.CacheMap

case class EstateAgentBusiness(
                                services: Option[Services] = None,
                                redressScheme: Option[RedressScheme] = None,
                                professionalBody: Option[ProfessionalBody] = None,
                                penalisedUnderEstateAgentsAct: Option[PenalisedUnderEstateAgentsAct] = None
                              ) {
  def services(p: Services): EstateAgentBusiness =
    this.copy(services = Some(p))

  def redressScheme(p: RedressScheme): EstateAgentBusiness =
    this.copy(redressScheme = Some(p))

  def professionalBody(p: ProfessionalBody): EstateAgentBusiness =
    this.copy(professionalBody = Some(p))

  def penalisedUnderEstateAgentsAct(p: PenalisedUnderEstateAgentsAct): EstateAgentBusiness =
    this.copy(penalisedUnderEstateAgentsAct = Some(p))

  def isComplete: Boolean =
    this match {
      case EstateAgentBusiness(Some(x), _, Some(_), Some(_)) if !x.services.contains(Residential) => true
      case EstateAgentBusiness(Some(_), Some(_), Some(_), Some(_)) => true
      case _ => false
    }
}

object EstateAgentBusiness {

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "eab"
    val incomplete = Section(messageKey, false, controllers.estateagentbusiness.routes.WhatYouNeedController.get())
    cache.getEntry[EstateAgentBusiness](key).fold(incomplete) {
      model =>
        if (model.isComplete) {
          Section(messageKey, true, controllers.estateagentbusiness.routes.SummaryController.get())
        } else {
          incomplete
        }
    }
  }

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  val key = "estate-agent-business"

  implicit val reads: Reads[EstateAgentBusiness] = (
    __.read[Option[Services]] and
    __.read[Option[RedressScheme]] and
      __.read[Option[ProfessionalBody]] and
      __.read[Option[PenalisedUnderEstateAgentsAct]]
    ) (EstateAgentBusiness.apply _)

  implicit val writes: Writes[EstateAgentBusiness] =
    Writes[EstateAgentBusiness] {
      model =>
        Seq(
          Json.toJson(model.services).asOpt[JsObject],
          Json.toJson(model.redressScheme).asOpt[JsObject],
          Json.toJson(model.professionalBody).asOpt[JsObject],
          Json.toJson(model.penalisedUnderEstateAgentsAct).asOpt[JsObject]
        ).flatten.fold(Json.obj()) {
          _ ++ _
        }
    }

  implicit def default(aboutYou: Option[EstateAgentBusiness]): EstateAgentBusiness =
    aboutYou.getOrElse(EstateAgentBusiness())
}
