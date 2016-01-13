package models.aboutyou

case class AboutYou(
                     yourDetails: Option[YourDetails] = None,
                     roleWithinBusiness: Option[RoleWithinBusiness] = None
                   ) {

  def yourDetails(v: YourDetails): AboutYou =
    this.copy(yourDetails = Some(v))

  def roleWithinBusiness(v: RoleWithinBusiness): AboutYou =
    this.copy(roleWithinBusiness = Some(v))
}

object AboutYou {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  val key = "about-you"

  implicit val reads: Reads[AboutYou] = (
    __.read[Option[YourDetails]] and
      __.read[Option[RoleWithinBusiness]]
    ) (AboutYou.apply _)

  implicit val writes: Writes[AboutYou] = Writes[AboutYou] {
    model =>
      Seq(
        Json.toJson(model.yourDetails).asOpt[JsObject],
        Json.toJson(model.roleWithinBusiness).asOpt[JsObject]
      ).flatten.fold(Json.obj()) {
        _ ++ _
      }
  }

  implicit def default(aboutYou: Option[AboutYou]): AboutYou =
    aboutYou.getOrElse(AboutYou())
}