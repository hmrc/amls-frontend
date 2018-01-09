/*
 * Copyright 2018 HM Revenue & Customs
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

package models.businessactivities

import config.ApplicationConfig
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import uk.gov.hmrc.http.cache.client.CacheMap
import models.businessactivities.TransactionTypes._

case class BusinessActivities(
                               involvedInOther: Option[InvolvedInOther] = None,
                               expectedBusinessTurnover: Option[ExpectedBusinessTurnover] = None,
                               expectedAMLSTurnover: Option[ExpectedAMLSTurnover] = None,
                               businessFranchise: Option[BusinessFranchise] = None,
                               transactionRecord: Option[Boolean] = None,
                               customersOutsideUK: Option[CustomersOutsideUK] = None,
                               ncaRegistered: Option[NCARegistered] = None,
                               accountantForAMLSRegulations: Option[AccountantForAMLSRegulations] = None,
                               identifySuspiciousActivity: Option[IdentifySuspiciousActivity] = None,
                               riskAssessmentPolicy: Option[RiskAssessmentPolicy] = None,
                               howManyEmployees: Option[HowManyEmployees] = None,
                               whoIsYourAccountant: Option[WhoIsYourAccountant] = None,
                               taxMatters: Option[TaxMatters] = None,
                               transactionRecordTypes: Option[TransactionTypes] = None,
                               hasChanged: Boolean = false,
                               hasAccepted: Boolean = false
                             ) {

  def businessFranchise(p: BusinessFranchise): BusinessActivities =
    this.copy(businessFranchise = Some(p), hasChanged = hasChanged || !this.businessFranchise.contains(p),
      hasAccepted = hasAccepted && this.businessFranchise.contains(p))

  def expectedAMLSTurnover(p: ExpectedAMLSTurnover): BusinessActivities =
    this.copy(expectedAMLSTurnover = Some(p), hasChanged = hasChanged || !this.expectedAMLSTurnover.contains(p),
      hasAccepted = hasAccepted && this.expectedAMLSTurnover.contains(p))

  def expectedBusinessTurnover(p: ExpectedBusinessTurnover): BusinessActivities =
    this.copy(expectedBusinessTurnover = Some(p), hasChanged = hasChanged || !this.expectedBusinessTurnover.contains(p),
      hasAccepted = hasAccepted && this.expectedBusinessTurnover.contains(p))

  def involvedInOther(p: InvolvedInOther): BusinessActivities =
    this.copy(involvedInOther = Some(p), hasChanged = hasChanged || !this.involvedInOther.contains(p),
      hasAccepted = hasAccepted && this.involvedInOther.contains(p))

  def identifySuspiciousActivity(p: IdentifySuspiciousActivity): BusinessActivities =
    this.copy(identifySuspiciousActivity = Some(p), hasChanged = hasChanged || !this.identifySuspiciousActivity.contains(p),
      hasAccepted = hasAccepted && this.identifySuspiciousActivity.contains(p))

  def transactionRecord(isRecorded: Boolean): BusinessActivities = {
    val types = if (isRecorded) this.transactionRecordTypes else None

    this.copy(transactionRecord = Some(isRecorded), transactionRecordTypes = types,
      hasChanged = hasChanged || !this.transactionRecord.contains(isRecorded),
      hasAccepted = hasAccepted && this.transactionRecord.contains(isRecorded))
  }

  def transactionRecordTypes(types: TransactionTypes): BusinessActivities =
    this.copy(transactionRecordTypes = Some(types), hasChanged = hasChanged || !this.transactionRecordTypes.contains(types),
      hasAccepted = hasAccepted && this.transactionRecordTypes.contains(types))

  def customersOutsideUK(p: CustomersOutsideUK): BusinessActivities =
    this.copy(customersOutsideUK = Some(p), hasChanged = hasChanged || !this.customersOutsideUK.contains(p),
      hasAccepted = hasAccepted && this.customersOutsideUK.contains(p))

  def ncaRegistered(p: NCARegistered): BusinessActivities =
    this.copy(ncaRegistered = Some(p), hasChanged = hasChanged || !this.ncaRegistered.contains(p),
      hasAccepted = hasAccepted && this.ncaRegistered.contains(p))

  def accountantForAMLSRegulations(p: Option[AccountantForAMLSRegulations]): BusinessActivities =
    this.copy(accountantForAMLSRegulations = p, hasChanged = hasChanged || !this.accountantForAMLSRegulations.contains(p),
      hasAccepted = hasAccepted && this.accountantForAMLSRegulations.contains(p))

  def riskAssessmentPolicy(p: RiskAssessmentPolicy): BusinessActivities =
    this.copy(riskAssessmentPolicy = Some(p), hasChanged = hasChanged || !this.riskAssessmentPolicy.contains(p),
      hasAccepted = hasAccepted && this.riskAssessmentPolicy.contains(p))

  def howManyEmployees(p: HowManyEmployees): BusinessActivities =
    this.copy(howManyEmployees = Some(p), hasChanged = hasChanged || !this.howManyEmployees.contains(p),
      hasAccepted = hasAccepted && this.howManyEmployees.contains(p))

  def whoIsYourAccountant(p: WhoIsYourAccountant): BusinessActivities =
    this.copy(whoIsYourAccountant = Some(p), hasChanged = hasChanged || !this.whoIsYourAccountant.contains(p),
      hasAccepted = hasAccepted && this.whoIsYourAccountant.contains(p))

  def taxMatters(p: TaxMatters): BusinessActivities =
    this.copy(taxMatters = Some(p), hasChanged = hasChanged || !this.taxMatters.contains(p),
      hasAccepted = hasAccepted && this.taxMatters.contains(p))


  def isComplete: Boolean = {
    if (ApplicationConfig.hasAcceptedToggle) {
      this match {
        case BusinessActivities(
        Some(_), _, Some(_), Some(_), Some(_), _,
        Some(_), _, Some(_), Some(_), Some(_), _, _, _, _, true) => true
        case BusinessActivities(
        Some(_), _, Some(_), Some(_), Some(_), _,
        Some(_), _, Some(_), Some(_), Some(_), _, _, _, _, false) => false
        case _ => false
      }
  } else {
      this match {
        case BusinessActivities(
        Some(_), _, Some(_), Some(_), Some(_), _,
        Some(_), _, Some(_), Some(_), Some(_), _, _, _, _, _) => true
        case _ => false
      }
    }

    }
}

object BusinessActivities {

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "businessactivities"
    val notStarted = Section(messageKey, NotStarted, false, controllers.businessactivities.routes.WhatYouNeedController.get())
    cache.getEntry[BusinessActivities](key).fold(notStarted) {
      model =>
        if (model.isComplete) {
          Section(messageKey, Completed, model.hasChanged, controllers.businessactivities.routes.SummaryController.get())
        } else {
          Section(messageKey, Started, model.hasChanged, controllers.businessactivities.routes.WhatYouNeedController.get())
        }
    }
  }

  import play.api.libs.functional.syntax._
  import play.api.libs.json._
  import utils.MappingUtils.constant

  val key = "business-activities"

  val transactionTypesReader = (__ \ "transactionTypes").read[TransactionTypes] map (Option(_))

  implicit val reads: Reads[BusinessActivities] = (
    __.read(Reads.optionNoError[InvolvedInOther]) and
      __.read(Reads.optionNoError[ExpectedBusinessTurnover]) and
      __.read(Reads.optionNoError[ExpectedAMLSTurnover]) and
      __.read(Reads.optionNoError[BusinessFranchise]) and
      (__ \ "isRecorded").readNullable[Boolean] and
      __.read(Reads.optionNoError[CustomersOutsideUK]) and
      __.read(Reads.optionNoError[NCARegistered]) and
      __.read(Reads.optionNoError[AccountantForAMLSRegulations]) and
      __.read(Reads.optionNoError[IdentifySuspiciousActivity]) and
      __.read(Reads.optionNoError[RiskAssessmentPolicy]) and
      __.read(Reads.optionNoError[HowManyEmployees]) and
      __.read(Reads.optionNoError[WhoIsYourAccountant]) and
      __.read(Reads.optionNoError[TaxMatters]) and
      (transactionTypesReader orElse TransactionTypes.oldTransactionTypeReader orElse constant(None)) and
      (__ \ "hasChanged").readNullable[Boolean].map(_.getOrElse(false)) and
      (__ \ "hasAccepted").readNullable[Boolean].map(_.getOrElse(false))
    ) (BusinessActivities.apply _)

  implicit val writes: Writes[BusinessActivities] = Writes[BusinessActivities] {
    model =>
      Seq(
        Json.toJson(model.involvedInOther).asOpt[JsObject],
        Json.toJson(model.expectedBusinessTurnover).asOpt[JsObject],
        Json.toJson(model.expectedAMLSTurnover).asOpt[JsObject],
        Json.toJson(model.businessFranchise).asOpt[JsObject],
        model.transactionRecord map { t => Json.obj("isRecorded" -> t)},
        model.transactionRecordTypes map (t => Json.obj("transactionTypes" -> Json.toJson(t))),
        Json.toJson(model.customersOutsideUK).asOpt[JsObject],
        Json.toJson(model.ncaRegistered).asOpt[JsObject],
        Json.toJson(model.accountantForAMLSRegulations).asOpt[JsObject],
        Json.toJson(model.identifySuspiciousActivity).asOpt[JsObject],
        Json.toJson(model.riskAssessmentPolicy).asOpt[JsObject],
        Json.toJson(model.howManyEmployees).asOpt[JsObject],
        Json.toJson(model.whoIsYourAccountant).asOpt[JsObject],
        Json.toJson(model.taxMatters).asOpt[JsObject]
      ).flatten.fold(Json.obj()) {
        _ ++ _
      } + ("hasChanged" -> JsBoolean(model.hasChanged)) + ("hasAccepted" -> JsBoolean(model.hasAccepted))
  }

  implicit def default(businessActivities: Option[BusinessActivities]): BusinessActivities =
    businessActivities.getOrElse(BusinessActivities())
}
