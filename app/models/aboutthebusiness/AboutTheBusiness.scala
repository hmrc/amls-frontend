package models.aboutthebusiness

case class AboutTheBusiness(
                             previouslyRegistered: Option[PreviouslyRegistered] = None,
                             vatRegistered: Option[VATRegistered] = None,
                             contactingYou: Option[ContactingYou] = None,
                             registeredOffice: Option[RegisteredOffice] = None
                     ) {

  def previouslyRegistered(v: PreviouslyRegistered): AboutTheBusiness =
    this.copy(previouslyRegistered = Some(v))

  def vatRegistered(r: VATRegistered): AboutTheBusiness =
    this.copy(vatRegistered = Some(r))

  def registeredOffice(v: RegisteredOffice): AboutTheBusiness =
    this.copy(registeredOffice = Some(v))

  def contactingYou(obj: ContactingYou):
  AboutTheBusiness = {
    this.copy(contactingYou = Some(obj))
  }
}

object AboutTheBusiness {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  val key = "about-the-business"
  implicit val reads: Reads[AboutTheBusiness] = (
      __.read[Option[PreviouslyRegistered]] and
        __.read[Option[VATRegistered]] and
        __.read[Option[ContactingYou]] and
        __.read[Option[RegisteredOffice]]
    ) (AboutTheBusiness.apply _)

  implicit val writes: Writes[AboutTheBusiness] = Writes[AboutTheBusiness] {
    model =>
      Seq(
        Json.toJson(model.previouslyRegistered).asOpt[JsObject],
        Json.toJson(model.vatRegistered).asOpt[JsObject],
        Json.toJson(model.contactingYou).asOpt[JsObject],
        Json.toJson(model.registeredOffice).asOpt[JsObject]
      ).flatten.fold(Json.obj()) {
        _ ++ _
      }
  }

  implicit def default(aboutYou: Option[AboutTheBusiness]): AboutTheBusiness =
    aboutYou.getOrElse(AboutTheBusiness())
}


