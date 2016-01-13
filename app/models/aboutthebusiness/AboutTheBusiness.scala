package models.aboutthebusiness

import scala.language.implicitConversions

case class AboutTheBusiness(
                             dummyModel: Option[BusinessWithVAT] = None,
                             businessWithVAT :Option[BusinessWithVAT] = None
                            ) {
  def dummyModel(obj:BusinessWithVAT) : AboutTheBusiness= {
    this.copy(dummyModel = Some(obj))
  }

  def businessWithVAT(obj:BusinessWithVAT) : AboutTheBusiness= {
    this.copy(businessWithVAT = Some(obj))
  }
}

object AboutTheBusiness {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  val key = "about-the-business"

  implicit val reads: Reads[AboutTheBusiness] = (
    __.read[Option[BusinessWithVAT]] and
    __.read[Option[BusinessWithVAT]]
    ) (AboutTheBusiness.apply _)

  implicit val writes: Writes[AboutTheBusiness] = Writes[AboutTheBusiness] {
    model =>
      Seq(
        Json.toJson(model.dummyModel).asOpt[JsObject]
      ).flatten.fold(Json.obj()) {
        _ ++ _
      }
  }

  implicit def default(aboutTheBusiness: Option[AboutTheBusiness]): AboutTheBusiness =
      aboutTheBusiness.getOrElse(AboutTheBusiness())

}