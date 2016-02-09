package models.businessmatching

case class BusinessMatching(
                                activities: Option[BusinessActivities] = None,
                                activities1: Option[BusinessActivities] = None
                           ) {
  def activities(ba: BusinessActivities): BusinessMatching =
    this.copy(activities = Some(ba))
}

object BusinessMatching {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  val key = "business-matching"

  implicit val reads: Reads[BusinessMatching] = (
      __.read[Option[BusinessActivities]] and
        __.read[Option[BusinessActivities]]
    ) (BusinessMatching.apply _)

  implicit val writes: Writes[BusinessMatching] =
    Writes[BusinessMatching] {
      model =>
        Seq(
          Json.toJson(model.activities).asOpt[JsObject],
          Json.toJson(model.activities1).asOpt[JsObject]
        ).flatten.fold(Json.obj()) {
          _ ++ _
        }
    }

  implicit def default(aboutYou: Option[BusinessMatching]): BusinessMatching =
    aboutYou.getOrElse(BusinessMatching())
}
