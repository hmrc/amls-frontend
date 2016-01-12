package models.aboutthebusiness

case class AboutTheBusiness(
                     previouslyRegistered: Option[PreviouslyRegistered] = None
                     ) {

  def previouslyRegistered(v: PreviouslyRegistered): AboutTheBusiness =
    this.copy(previouslyRegistered = Some(v))
}

object AboutTheBusiness {

  import play.api.libs.json._

  val key = "about-the-business"

  implicit val reads: Reads[AboutTheBusiness] = (
      __.read[Option[PreviouslyRegistered]]
    ) (AboutTheBusiness.apply _)

  implicit val writes: Writes[AboutTheBusiness] = Writes[AboutTheBusiness] {
    model =>
      Seq(
        Json.toJson(model.previouslyRegistered).asOpt[JsObject]
      ).flatten.fold(Json.obj()) {
        _ ++ _
      }
  }

  implicit def default(aboutYou: Option[AboutTheBusiness]): AboutTheBusiness =
    aboutYou.getOrElse(AboutTheBusiness())
}


