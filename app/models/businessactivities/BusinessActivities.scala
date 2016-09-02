package models.businessactivities

import models.registrationprogress.{Completed, NotStarted, Section, Started}
import uk.gov.hmrc.http.cache.client.CacheMap

case class BusinessActivities(
                               involvedInOther: Option[InvolvedInOther] = None,
                               expectedBusinessTurnover: Option[ExpectedBusinessTurnover] = None,
                               expectedAMLSTurnover: Option[ExpectedAMLSTurnover] = None,
                               businessFranchise: Option[BusinessFranchise] = None,
                               transactionRecord: Option[TransactionRecord] = None,
                               customersOutsideUK: Option[CustomersOutsideUK] = None,
                               ncaRegistered: Option[NCARegistered] = None,
                               accountantForAMLSRegulations: Option[AccountantForAMLSRegulations] = None,
                               identifySuspiciousActivity: Option[IdentifySuspiciousActivity] = None,
                               riskAssessmentPolicy: Option[RiskAssessmentPolicy] = None,
                               howManyEmployees: Option[HowManyEmployees] = None,
                               whoIsYourAccountant: Option[WhoIsYourAccountant] = None,
                               hasChanged : Boolean = false
                             ) {

  def businessFranchise(p: BusinessFranchise): BusinessActivities =
    this.copy(businessFranchise = Some(p), hasChanged = hasChanged || !this.businessFranchise.contains(p))

  def expectedAMLSTurnover(p: ExpectedAMLSTurnover): BusinessActivities =
    this.copy(expectedAMLSTurnover = Some(p), hasChanged = hasChanged || !this.expectedAMLSTurnover.contains(p))

  def expectedBusinessTurnover(p: ExpectedBusinessTurnover): BusinessActivities =
    this.copy(expectedBusinessTurnover = Some(p), hasChanged = hasChanged || !this.expectedBusinessTurnover.contains(p))

  def involvedInOther(p: InvolvedInOther): BusinessActivities =
    this.copy(involvedInOther = Some(p), hasChanged = hasChanged || !this.involvedInOther.contains(p))

  def identifySuspiciousActivity(p: IdentifySuspiciousActivity): BusinessActivities =
    this.copy(identifySuspiciousActivity = Some(p), hasChanged = hasChanged || !this.identifySuspiciousActivity.contains(p))

  def transactionRecord(p: TransactionRecord): BusinessActivities =
    this.copy(transactionRecord = Some(p), hasChanged = hasChanged || !this.transactionRecord.contains(p))

  def customersOutsideUK(p: CustomersOutsideUK): BusinessActivities =
    this.copy(customersOutsideUK = Some(p), hasChanged = hasChanged || !this.customersOutsideUK.contains(p))

  def ncaRegistered(p: NCARegistered): BusinessActivities =
    this.copy(ncaRegistered = Some(p), hasChanged = hasChanged || !this.ncaRegistered.contains(p))

  def accountantForAMLSRegulations(p: AccountantForAMLSRegulations): BusinessActivities =
    this.copy(accountantForAMLSRegulations = Some(p), hasChanged = hasChanged || !this.accountantForAMLSRegulations.contains(p))

  def riskAssessmentPolicy(p: RiskAssessmentPolicy): BusinessActivities =
    this.copy(riskAssessmentPolicy = Some(p), hasChanged = hasChanged || !this.riskAssessmentPolicy.contains(p))

  def howManyEmployees(p: HowManyEmployees): BusinessActivities =
    this.copy(howManyEmployees = Some(p), hasChanged = hasChanged || !this.howManyEmployees.contains(p))

  def whoIsYourAccountant(p: WhoIsYourAccountant): BusinessActivities =
    this.copy(whoIsYourAccountant = Some(p), hasChanged = hasChanged || !this.whoIsYourAccountant.contains(p))

  def isComplete: Boolean =
    this match {
      case BusinessActivities(
      Some(_), _, Some(_), Some(_), Some(_), Some(_),
      Some(_), Some(_), Some(_), Some(_), Some(_), _, _
      ) => true
      case _ => false
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

  val key = "business-activities"

  implicit val reads: Reads[BusinessActivities] = (
    __.read[Option[InvolvedInOther]] and
      __.read[Option[ExpectedBusinessTurnover]] and
      __.read[Option[ExpectedAMLSTurnover]] and
      __.read[Option[BusinessFranchise]] and
      __.read[Option[TransactionRecord]] and
      __.read[Option[CustomersOutsideUK]] and
      __.read[Option[NCARegistered]] and
      __.read[Option[AccountantForAMLSRegulations]] and
      __.read[Option[IdentifySuspiciousActivity]] and
      __.read[Option[RiskAssessmentPolicy]] and
      __.read[Option[HowManyEmployees]] and
      __.read[Option[WhoIsYourAccountant]] and
      (__ \ "hasChanged").readNullable[Boolean].map(_.getOrElse(false))
    ) (BusinessActivities.apply _)

  implicit val writes: Writes[BusinessActivities] = Writes[BusinessActivities] {
    model =>
      Seq(
        Json.toJson(model.involvedInOther).asOpt[JsObject],
        Json.toJson(model.expectedBusinessTurnover).asOpt[JsObject],
        Json.toJson(model.expectedAMLSTurnover).asOpt[JsObject],
        Json.toJson(model.businessFranchise).asOpt[JsObject],
        Json.toJson(model.transactionRecord).asOpt[JsObject],
        Json.toJson(model.customersOutsideUK).asOpt[JsObject],
        Json.toJson(model.ncaRegistered).asOpt[JsObject],
        Json.toJson(model.accountantForAMLSRegulations).asOpt[JsObject],
        Json.toJson(model.identifySuspiciousActivity).asOpt[JsObject],
        Json.toJson(model.riskAssessmentPolicy).asOpt[JsObject],
        Json.toJson(model.howManyEmployees).asOpt[JsObject],
        Json.toJson(model.whoIsYourAccountant).asOpt[JsObject],
        Json.toJson(model.hasChanged).asOpt[JsObject]
      ).flatten.fold(Json.obj()) {
        _ ++ _
      }
  }

  implicit def default(businessActivities: Option[BusinessActivities]): BusinessActivities =
    businessActivities.getOrElse(BusinessActivities())
}
