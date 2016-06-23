package models.businessmatching

import models.businesscustomer.ReviewDetails
import models.businessmatching.BusinessType.{LPrLLP, LimitedCompany, UnincorporatedBody}
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import uk.gov.hmrc.http.cache.client.CacheMap

case class BusinessMatching(
                             reviewDetails: Option[ReviewDetails] = None,
                             activities: Option[BusinessActivities] = None,
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

  def isComplete: Boolean =
    this match {
      case BusinessMatching(Some(x), Some(_), Some(_), _)
        if x.businessType.fold(false) { _ == UnincorporatedBody } => true
      case BusinessMatching(Some(x), Some(_), _, Some(_))
        if x.businessType.fold(false) { y => y == LimitedCompany || y == LPrLLP } => true
      case BusinessMatching(Some(_), Some(_), None, None) => true
      case _ => false
    }
}

object BusinessMatching {

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "businessmatching"
    val incomplete =
      Section(messageKey, NotStarted, controllers.businessmatching.routes.RegisterServicesController.get())
    cache.getEntry[BusinessMatching](key).fold(incomplete) {
      model =>
        if (model.isComplete) {
          Section(messageKey, Completed, controllers.businessmatching.routes.SummaryController.get())
        } else {
          Section(messageKey, Started, controllers.businessmatching.routes.RegisterServicesController.get())
        }
    }
  }

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  val key = "business-matching"

    implicit val reads: Reads[BusinessMatching] = (
        __.read[Option[ReviewDetails]] and
        __.read[Option[BusinessActivities]] and
        __.read[Option[TypeOfBusiness]] and
        __.read[Option[CompanyRegistrationNumber]]
      ) (BusinessMatching.apply _)

  implicit val writes: Writes[BusinessMatching] =
    Writes[BusinessMatching] {
      model =>
        Seq(
          Json.toJson(model.reviewDetails).asOpt[JsObject],
          Json.toJson(model.activities).asOpt[JsObject],
          Json.toJson(model.typeOfBusiness).asOpt[JsObject],
          Json.toJson(model.companyRegistrationNumber).asOpt[JsObject]
        ).flatten.fold(Json.obj()) {
          _ ++ _
        }
    }

  implicit def default(businessMatching: Option[BusinessMatching]): BusinessMatching =
    businessMatching.getOrElse(BusinessMatching())
}
