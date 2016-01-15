package models.aboutthebusiness

import play.api.libs.json.Json

case class AboutTheBusiness(
                             previouslyRegistered: Option[PreviouslyRegistered] = None,
                             registeredForVAT: Option[RegisteredForVAT] = None
                     ) {

  def previouslyRegistered(v: PreviouslyRegistered): AboutTheBusiness =
    this.copy(previouslyRegistered = Some(v))

  def registeredForVAT(r: RegisteredForVAT): AboutTheBusiness =
    this.copy(registeredForVAT = Some(r))
}

object AboutTheBusiness {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  val key = "about-the-business"
  implicit val reads: Reads[AboutTheBusiness] = (
      __.read[Option[PreviouslyRegistered]] and
        __.read[Option[RegisteredForVAT]]
    ) (AboutTheBusiness.apply _)

  implicit val writes: Writes[AboutTheBusiness] = Writes[AboutTheBusiness] {
    model =>
      Seq(
        Json.toJson(model.previouslyRegistered).asOpt[JsObject],
        Json.toJson(model.registeredForVAT).asOpt[JsObject]
      ).flatten.fold(Json.obj()) {
        _ ++ _
      }
  }

  implicit def default(aboutYou: Option[AboutTheBusiness]): AboutTheBusiness =
    aboutYou.getOrElse(AboutTheBusiness())
}


