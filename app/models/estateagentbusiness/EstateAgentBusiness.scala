package models.estateagentbusiness

case class EstateAgentBusiness(
                                services: Option[Set[Service]] = None,
                                estateAgentAct: Option[String] = None,
                                professionalBody: Option[String] = None
                              )

object EstateAgentBusiness {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  val key = "estate-agent-business"

//  implicit val reads: Reads[EstateAgentBusiness] = (
//    __.read[Option[Set[Service]]] and
//      __.read[Option[String]] and
//      __.read[Option[String]]
//    ) (EstateAgentBusiness.apply _)

//  implicit val writes: Writes[EstateAgentBusiness] =
//    Writes[EstateAgentBusiness] {
//      model =>
//        Seq(
//          Json.toJson(model.services).asOpt[JsObject],
//          Json.toJson(model.estateAgentAct).asOpt[JsObject],
//            Json.toJson(model.professionalBody).asOpt[JsObject]
//        ).flatten.fold(Json.obj()) {
//          _ ++ _
//        }
//    }

  implicit def default(aboutYou: Option[EstateAgentBusiness]): EstateAgentBusiness =
    aboutYou.getOrElse(EstateAgentBusiness())
}
