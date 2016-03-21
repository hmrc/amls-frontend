package models.businessmatching

import models.businesscustomer.ReviewDetails

case class BusinessMatching(
                             activities: Option[BusinessActivities] = None,
                             reviewDetails: Option[ReviewDetails] = None,
                             typeOfBusiness: Option[TypeOfBusiness] = None,
                             companyRegistrationNumber: Option[CompanyRegistrationNumber] = None
                           ) {
  def activities(ba: BusinessActivities): BusinessMatching =
    this.copy(activities = Some(ba))

  def reviewDetails(s: ReviewDetails): BusinessMatching =
    this.copy(reviewDetails = Some(s))

  def typeOfBusiness(b: TypeOfBusiness): BusinessMatching =
    this.copy(typeOfBusiness = Some(b))

  def companyRegistrationNumber(crn: CompanyRegistrationNumber): BusinessMatching =
    this.copy(companyRegistrationNumber = Some(crn))

}

object BusinessMatching {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  val key = "business-matching"

    implicit val reads: Reads[BusinessMatching] = (
        __.read[Option[BusinessActivities]] and
        __.read[Option[ReviewDetails]] and
        __.read[Option[TypeOfBusiness]] and
        __.read[Option[CompanyRegistrationNumber]]
      ) (BusinessMatching.apply _)

  implicit val writes: Writes[BusinessMatching] =
    Writes[BusinessMatching] {
      model =>
        Seq(
          Json.toJson(model.activities).asOpt[JsObject],
          Json.toJson(model.reviewDetails).asOpt[JsObject],
          Json.toJson(model.typeOfBusiness).asOpt[JsObject],
          Json.toJson(model.companyRegistrationNumber).asOpt[JsObject]
        ).flatten.fold(Json.obj()) {
          _ ++ _
        }
    }

  implicit def default(businessMatching: Option[BusinessMatching]): BusinessMatching =
    businessMatching.getOrElse(BusinessMatching())
}
