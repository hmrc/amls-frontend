package models.businessmatching

import models.businesscustomer.ReviewDetails
import models.businessmatching.BusinessType.{LPrLLP, LimitedCompany, UnincorporatedBody}
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import uk.gov.hmrc.http.cache.client.CacheMap

case class BusinessMatching(
                             reviewDetails: Option[ReviewDetails] = None,
                             activities: Option[BusinessActivities] = None,
                             typeOfBusiness: Option[TypeOfBusiness] = None,
                             companyRegistrationNumber: Option[CompanyRegistrationNumber] = None,
                             hasChanged: Boolean = false
                           ) {

  def activities(p: BusinessActivities): BusinessMatching =
    this.copy(activities = Some(p), hasChanged = hasChanged || !this.activities.contains(p))

  def reviewDetails(p: ReviewDetails): BusinessMatching =
    this.copy(reviewDetails = Some(p), hasChanged = hasChanged || !this.reviewDetails.contains(p))

  def typeOfBusiness(p: TypeOfBusiness): BusinessMatching =
    this.copy(typeOfBusiness = Some(p), hasChanged = hasChanged || !this.typeOfBusiness.contains(p))

  def companyRegistrationNumber(p: CompanyRegistrationNumber): BusinessMatching =
    this.copy(companyRegistrationNumber = Some(p), hasChanged = hasChanged || !this.companyRegistrationNumber.contains(p))

  def isComplete: Boolean =
    this match {
      case BusinessMatching(Some(x), Some(_), Some(_), _, _)
        if x.businessType.fold(false) { _ == UnincorporatedBody } => true
      case BusinessMatching(Some(x), Some(_), _, Some(_), _)
        if x.businessType.fold(false) { y => y == LimitedCompany || y == LPrLLP } => true
      case BusinessMatching(Some(_), Some(_), None, None, _) => true
      case _ => false
    }
}

object BusinessMatching {

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "businessmatching"
    val incomplete = Section(messageKey, NotStarted, false, controllers.businessmatching.routes.RegisterServicesController.get())
    cache.getEntry[BusinessMatching](key).fold(incomplete) {
      model =>
        if (model.isComplete) {
          Section(messageKey, Completed, model.hasChanged, controllers.businessmatching.routes.SummaryController.get(true))
        } else {
          Section(messageKey, Started, model.hasChanged, controllers.businessmatching.routes.RegisterServicesController.get())
        }
    }
  }

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  val key = "business-matching"

  implicit val reads: Reads[BusinessMatching] = (
    (__ \ "reviewDetails").readNullable[ReviewDetails] and
      (__ \ "activities").readNullable[BusinessActivities] and
      (__ \ "typeOfBusiness").readNullable[TypeOfBusiness] and
      (__ \ "companyRegistrationNumber").readNullable[CompanyRegistrationNumber] and
      (__ \ "hasChanged").readNullable[Boolean].map {_.getOrElse(false)}
    ) apply BusinessMatching.apply _

  implicit val writes: Writes[BusinessMatching] = Json.writes[BusinessMatching]

  implicit def default(businessMatching: Option[BusinessMatching]): BusinessMatching =
    businessMatching.getOrElse(BusinessMatching())
}
