package models.estateagentbusiness

import models.registrationprogress.{Completed, NotStarted, Section, Started}
import uk.gov.hmrc.http.cache.client.CacheMap

case class EstateAgentBusiness(
                                services: Option[Services] = None,
                                redressScheme: Option[RedressScheme] = None,
                                professionalBody: Option[ProfessionalBody] = None,
                                penalisedUnderEstateAgentsAct: Option[PenalisedUnderEstateAgentsAct] = None,
                                hasChanged : Boolean = false
                              ) {
  def services(p: Services): EstateAgentBusiness =
    this.copy(services = Some(p), hasChanged = hasChanged || !this.services.contains(p))

  def redressScheme(p: RedressScheme): EstateAgentBusiness =
    this.copy(redressScheme = Some(p), hasChanged = hasChanged || !this.redressScheme.contains(p))

  def professionalBody(p: ProfessionalBody): EstateAgentBusiness =
    this.copy(professionalBody = Some(p), hasChanged = hasChanged || !this.professionalBody.contains(p))

  def penalisedUnderEstateAgentsAct(p: PenalisedUnderEstateAgentsAct): EstateAgentBusiness =
    this.copy(penalisedUnderEstateAgentsAct = Some(p), hasChanged = hasChanged || !this.penalisedUnderEstateAgentsAct.contains(p))

  def isComplete: Boolean =
    this match {
      case EstateAgentBusiness(Some(x), _, Some(_), Some(_), _) if !x.services.contains(Residential) => true
      case EstateAgentBusiness(Some(_), Some(_), Some(_), Some(_), _) => true
      case _ => false
    }
}

object EstateAgentBusiness {

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "eab"
    val notStarted = Section(messageKey, NotStarted, false, controllers.estateagentbusiness.routes.WhatYouNeedController.get())

    cache.getEntry[EstateAgentBusiness](key).fold(notStarted) {
      model =>
        if (model.isComplete) {
          Section(messageKey, Completed, model.hasChanged, controllers.estateagentbusiness.routes.SummaryController.get(true))
        } else {
          Section(messageKey, Started, model.hasChanged, controllers.estateagentbusiness.routes.WhatYouNeedController.get())
        }
    }
  }

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  val key = "estate-agent-business"

  implicit val reads: Reads[EstateAgentBusiness] = (
    (__ \ "services").readNullable[Services] and
    (__ \ "redressScheme").readNullable[RedressScheme] and
    (__ \ "professionalBody").readNullable[ProfessionalBody] and
    (__ \ "penalisedUnderEstateAgentsAct").readNullable[PenalisedUnderEstateAgentsAct] and
      (__ \ "hasChanged").readNullable[Boolean].map {_.getOrElse(false)}
    ) apply EstateAgentBusiness.apply _

  implicit val writes: Writes[EstateAgentBusiness] = Json.writes[EstateAgentBusiness]

  implicit def default(aboutYou: Option[EstateAgentBusiness]): EstateAgentBusiness =
    aboutYou.getOrElse(EstateAgentBusiness())
}
