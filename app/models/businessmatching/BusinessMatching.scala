package models.businessmatching

case class BusinessMatching (activities: Option[BusinessActivities] = None) {
  def activities(ba: BusinessActivities): BusinessMatching =
    this.copy(activities = Some(ba))
}

object BusinessMatching {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  val key = "business-matching"

  //  implicit val reads: Reads[BusinessMatching] = (
  //      __.read[Option[BusinessActivities]] and
  //    ) (BusinessMatching.apply _)

  // TODO: Revert to the normal pattern when there are more properties added to the model
  implicit val reads: Reads[BusinessMatching] =
    __.read[Option[BusinessActivities]] map (BusinessMatching.apply)

  implicit val writes: Writes[BusinessMatching] =
    Writes[BusinessMatching] {
      model =>
        Seq(
          Json.toJson(model.activities).asOpt[JsObject]
        ).flatten.fold(Json.obj()) {
          _ ++ _
        }
    }

  implicit def default(aboutYou: Option[BusinessMatching]): BusinessMatching =
    aboutYou.getOrElse(BusinessMatching())
}
