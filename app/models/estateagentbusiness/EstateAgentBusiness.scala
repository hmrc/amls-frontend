package models.estateagentbusiness

case class EstateAgentBusiness(
                                estateAgentAct: Option[String] = None,
                                professionalBody: Option[ProfessionalBody] = None
                              )
{
  def professionalBody(p: ProfessionalBody): EstateAgentBusiness =
    this.copy(professionalBody = Some(p))
}

object EstateAgentBusiness {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  val key = "estate-agent-business"

  implicit val reads: Reads[EstateAgentBusiness] = (
      __.read[Option[String]] and
      __.read[Option[ProfessionalBody]]
    ) (EstateAgentBusiness.apply _)

  implicit val writes: Writes[EstateAgentBusiness] =
    Writes[EstateAgentBusiness] {
      model =>
        Seq(
          Json.toJson(model.estateAgentAct).asOpt[JsObject],
            Json.toJson(model.professionalBody).asOpt[JsObject]
        ).flatten.fold(Json.obj()) {
          _ ++ _
        }
    }

  implicit def default(aboutYou: Option[EstateAgentBusiness]): EstateAgentBusiness =
    aboutYou.getOrElse(EstateAgentBusiness())
}
