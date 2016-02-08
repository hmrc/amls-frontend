package models.estateagentbusiness

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

}

object EstateAgentBusiness {

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
