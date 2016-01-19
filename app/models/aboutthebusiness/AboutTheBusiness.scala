package models.aboutthebusiness

case class AboutTheBusiness(
                             previouslyRegistered: Option[PreviouslyRegistered] = None,
                             vatRegistered: Option[VATRegistered] = None,
                             contactingYou: Option[ContactingYou] = None,
                             registeredOffice: Option[RegisteredOffice] = None,
                             correspondenceAddress: Option[CorrespondenceAddress] = None
                           ) {

  def previouslyRegistered(v: PreviouslyRegistered): AboutTheBusiness =
    this.copy(previouslyRegistered = Some(v))

  def vatRegistered(v: VATRegistered): AboutTheBusiness =
    this.copy(vatRegistered = Some(v))

  def registeredOffice(v: RegisteredOffice): AboutTheBusiness =
    this.copy(registeredOffice = Some(v))

  def contactingYou(v: ContactingYou): AboutTheBusiness =
    this.copy(contactingYou = Some(v))

  def correspondenceAddress(v: CorrespondenceAddress): AboutTheBusiness =
    this.copy(correspondenceAddress = Some(v))
}

object AboutTheBusiness {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  val key = "about-the-business"
  implicit val reads: Reads[AboutTheBusiness] = (
    __.read[Option[PreviouslyRegistered]] and
      __.read[Option[VATRegistered]] and
      __.read[Option[ContactingYou]] and
      __.read[Option[RegisteredOffice]] and
      __.read[Option[CorrespondenceAddress]]
    ) (AboutTheBusiness.apply _)

  implicit val writes: Writes[AboutTheBusiness] = Writes[AboutTheBusiness] {
    model =>
      Seq(
        Json.toJson(model.previouslyRegistered).asOpt[JsObject],
        Json.toJson(model.vatRegistered).asOpt[JsObject],
        Json.toJson(model.contactingYou).asOpt[JsObject],
        Json.toJson(model.registeredOffice).asOpt[JsObject],
        Json.toJson(model.correspondenceAddress).asOpt[JsObject]
      ).flatten.fold(Json.obj()) {
        _ ++ _
      }
  }

  implicit def default(aboutYou: Option[AboutTheBusiness]): AboutTheBusiness =
    aboutYou.getOrElse(AboutTheBusiness())
}


