package models.businessmatching

import models.businesscustomer.ReviewDetails
import models.businessmatching.BusinessType.{LPrLLP, LimitedCompany, UnincorporatedBody}
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import uk.gov.hmrc.http.cache.client.CacheMap

case class BusinessMatching(
                             reviewDetails: Option[ReviewDetails] = None,
                             activities: Option[BusinessActivities] = None,
                             msbServices : Option[MsbServices] = None,
                             typeOfBusiness: Option[TypeOfBusiness] = None,
                             companyRegistrationNumber: Option[CompanyRegistrationNumber] = None,
                             businessAppliedForPSRNumber: Option[BusinessAppliedForPSRNumber] = None,
                             hasChanged: Boolean = false
                           ) {

  def activities(p: BusinessActivities): BusinessMatching =
    this.copy(activities = Some(p), hasChanged = hasChanged || !this.activities.contains(p))

  def msbServices(p: MsbServices): BusinessMatching =
    this.copy(msbServices = Some(p), hasChanged = hasChanged || !this.msbServices.contains(p))

  def reviewDetails(p: ReviewDetails): BusinessMatching =
    this.copy(reviewDetails = Some(p), hasChanged = hasChanged || !this.reviewDetails.contains(p))

  def typeOfBusiness(p: TypeOfBusiness): BusinessMatching =
    this.copy(typeOfBusiness = Some(p), hasChanged = hasChanged || !this.typeOfBusiness.contains(p))

  def companyRegistrationNumber(p: CompanyRegistrationNumber): BusinessMatching =
    this.copy(companyRegistrationNumber = Some(p), hasChanged = hasChanged || !this.companyRegistrationNumber.contains(p))

  def businessAppliedForPSRNumber(p: BusinessAppliedForPSRNumber): BusinessMatching = {
    this.copy(businessAppliedForPSRNumber = Some(p), hasChanged = hasChanged || !this.businessAppliedForPSRNumber.contains(p))
  }

  def msbComplete(activities: BusinessActivities): Boolean = {
    if(activities.businessActivities.contains(MoneyServiceBusiness)) {
      this.msbServices.isDefined && this.msbServices.fold(false)(x => x.services.contains(TransmittingMoney) match {
        case true => this.businessAppliedForPSRNumber.isDefined
        case false => true
      })
    } else {
      true
    }
  }
  def isComplete: Boolean =
    this match {
      case BusinessMatching(Some(x), Some(activity), _,Some(_), _, _, _)
        if x.businessType.fold(false) {
          _ == UnincorporatedBody
        } && msbComplete(activity) => true
      case BusinessMatching(Some(x), Some(activity), _, _, Some(_), _, _)
        if x.businessType.fold(false) { y => y == LimitedCompany || y == LPrLLP } && msbComplete(activity) => true
      case BusinessMatching(Some(_),Some(activity),_, None, None, _, _)  if msbComplete(activity) => true
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
          Section(messageKey, Completed, model.hasChanged, controllers.businessmatching.routes.SummaryController.get())
        } else {
          Section(messageKey, Started, model.hasChanged, controllers.businessmatching.routes.RegisterServicesController.get())
        }
    }
  }

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  val key = "business-matching"

    implicit val reads: Reads[BusinessMatching] = (
        __.read[Option[ReviewDetails]] and
        __.read[Option[BusinessActivities]] and
        __.read[Option[MsbServices]] and
        __.read[Option[TypeOfBusiness]] and
        __.read[Option[CompanyRegistrationNumber]] and
          __.read[Option[BusinessAppliedForPSRNumber]] and
      (__ \ "hasChanged").readNullable[Boolean].map(_.getOrElse(false))
      ) (BusinessMatching.apply _)

  implicit val writes: Writes[BusinessMatching] =
    Writes[BusinessMatching] {
      model =>
        Seq(
          Json.toJson(model.reviewDetails).asOpt[JsObject],
          Json.toJson(model.activities).asOpt[JsObject],
          Json.toJson(model.msbServices).asOpt[JsObject],
          Json.toJson(model.typeOfBusiness).asOpt[JsObject],
          Json.toJson(model.companyRegistrationNumber).asOpt[JsObject],
          Json.toJson(model.businessAppliedForPSRNumber).asOpt[JsObject]
        ).flatten.fold(Json.obj()) {
          _ ++ _
        } + ("hasChanged" -> JsBoolean(model.hasChanged))
    }

  implicit def default(businessMatching: Option[BusinessMatching]): BusinessMatching =
    businessMatching.getOrElse(BusinessMatching())
}
