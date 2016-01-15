package models.aboutthebusiness

case class AboutTheBusiness(
                             previouslyRegistered: Option[PreviouslyRegistered] = None,
                             contactingYou: Option[ContactingYou] = None
                     ) {

  def previouslyRegistered(v: PreviouslyRegistered): AboutTheBusiness =
    this.copy(previouslyRegistered = Some(v))

   def contactingYou(obj: ContactingYou):
  AboutTheBusiness = {
    this.copy(contactingYou = Some(obj))
  }
}

object AboutTheBusiness {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  val key = "about-the-business"
  implicit val reads: Reads[AboutTheBusiness] = (
      __.read[Option[PreviouslyRegistered]] and
        __.read[Option[ContactingYou]]

    ) (AboutTheBusiness.apply _)

  implicit val writes: Writes[AboutTheBusiness] = Writes[AboutTheBusiness] {
    model =>
      Seq(
        Json.toJson(model.previouslyRegistered).asOpt[JsObject],
        Json.toJson(model.contactingYou).asOpt[JsObject]
      ).flatten.fold(Json.obj()) {
        _ ++ _
      }
  }

  implicit def default(aboutYou: Option[AboutTheBusiness]): AboutTheBusiness =
    aboutYou.getOrElse(AboutTheBusiness())
}


