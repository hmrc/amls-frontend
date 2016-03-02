package models.businessactivities

case class BusinessActivities(
                               involvedInOther: Option[InvolvedInOther] = None,
                               expectedAMLSTurnover: Option[ExpectedAMLSTurnover] = None,
                               businessFranchise: Option[BusinessFranchise] = None
                               ) {
  def businessFranchise(p: BusinessFranchise): BusinessActivities =
    this.copy(businessFranchise = Some(p))

  def expectedAMLSTurnover(p: ExpectedAMLSTurnover): BusinessActivities =
    this.copy(expectedAMLSTurnover = Some(p))


  def involvedInOther(p: InvolvedInOther): BusinessActivities =
    this.copy(involvedInOther = Some(p))
}

object BusinessActivities {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  val key = "business-activities"

  implicit val reads: Reads[BusinessActivities] = (
    __.read[Option[InvolvedInOther]] and
    __.read[Option[ExpectedAMLSTurnover]] and
    __.read[Option[BusinessFranchise]]
    )(BusinessActivities.apply _)

  implicit val writes: Writes[BusinessActivities] = Writes[BusinessActivities] {
  model =>
    Seq(
      Json.toJson(model.involvedInOther).asOpt[JsObject],
      Json.toJson(model.expectedAMLSTurnover).asOpt[JsObject],
      Json.toJson(model.businessFranchise).asOpt[JsObject]
    ).flatten.fold(Json.obj()){
     _ ++ _
    }
  }

  implicit def default(businessActivities: Option[BusinessActivities]): BusinessActivities =
    businessActivities.getOrElse(BusinessActivities())
}