/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models.businessmatching

import models.businesscustomer.ReviewDetails
import models.businessmatching.BusinessType.{LPrLLP, LimitedCompany, UnincorporatedBody}
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import uk.gov.hmrc.http.cache.client.CacheMap

case class BusinessMatching(
                             reviewDetails: Option[ReviewDetails] = None,
                             activities: Option[BusinessActivities] = None,
                             msbServices: Option[MsbServices] = None,
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
    if (activities.businessActivities.contains(MoneyServiceBusiness)) {
      this.msbServices.isDefined && this.msbServices.fold(false)(_.msbServices.contains(TransmittingMoney) match {
        case true => this.businessAppliedForPSRNumber.isDefined
        case false => true
      })
    } else {
      true
    }
  }

  def isbusinessTypeComplete(businessType: Option[BusinessType]): Boolean = {
    businessType.fold(true) {
      case LimitedCompany | LPrLLP => this.companyRegistrationNumber.isDefined
      case UnincorporatedBody => this.typeOfBusiness.isDefined
      case _ => true
    }
  }

  def isComplete: Boolean =
    this match {
      case BusinessMatching(Some(x), Some(activity), _, _, _, _, _)
        if {
          isbusinessTypeComplete(x.businessType) && msbComplete(activity)
        } => true
      case _ => false
    }
}

object BusinessMatching {

  val messageKey = "businessmatching"

  def section(implicit cache: CacheMap): Section = {
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
    __.read(Reads.optionNoError[ReviewDetails]) and
      __.read(Reads.optionNoError[BusinessActivities]) and
      __.read(Reads.optionNoError[MsbServices]) and
      __.read(Reads.optionNoError[TypeOfBusiness]) and
      __.read(Reads.optionNoError[CompanyRegistrationNumber]) and
      __.read(Reads.optionNoError[BusinessAppliedForPSRNumber]) and
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
