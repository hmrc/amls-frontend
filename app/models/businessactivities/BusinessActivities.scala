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
                               whoIsYourAccountant: Option[WhoIsYourAccountant] = None
                             ) {

  def businessFranchise(p: BusinessFranchise): BusinessActivities =
    this.copy(businessFranchise = Some(p))

  def expectedAMLSTurnover(p: ExpectedAMLSTurnover): BusinessActivities =
    this.copy(expectedAMLSTurnover = Some(p))

  def expectedBusinessTurnover(p: ExpectedBusinessTurnover): BusinessActivities =
    this.copy(expectedBusinessTurnover = Some(p))

  def involvedInOther(p: InvolvedInOther): BusinessActivities =
    this.copy(involvedInOther = Some(p))

  def identifySuspiciousActivity(p: IdentifySuspiciousActivity): BusinessActivities =
    this.copy(identifySuspiciousActivity = Some(p))

  def transactionRecord(p: TransactionRecord): BusinessActivities =
    this.copy(transactionRecord = Some(p))

  def customersOutsideUK(p: CustomersOutsideUK): BusinessActivities =
    this.copy(customersOutsideUK = Some(p))

  def ncaRegistered(p: NCARegistered): BusinessActivities =
    this.copy(ncaRegistered = Some(p))

  def accountantForAMLSRegulations(p: AccountantForAMLSRegulations): BusinessActivities =
    this.copy(accountantForAMLSRegulations = Some(p))

  def riskAssessmentspolicy(p: RiskAssessmentPolicy): BusinessActivities =
    this.copy(riskAssessmentPolicy = Some(p))

  def employees(p: HowManyEmployees): BusinessActivities =
    this.copy(howManyEmployees = Some(p))

  def whoIsYourAccountant(p: WhoIsYourAccountant): BusinessActivities =
    this.copy(whoIsYourAccountant = Some(p))

  def isComplete: Boolean =
    this match {
      case BusinessActivities(
      Some(_), _, Some(_), Some(_), Some(_), Some(_),
      Some(_), Some(x), Some(_), Some(_), Some(_), _
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
          Section(messageKey, Completed, false, controllers.businessactivities.routes.SummaryController.get())
        } else {
          Section(messageKey, Started, false, controllers.businessactivities.routes.WhatYouNeedController.get())
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
      __.read[Option[WhoIsYourAccountant]]
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
        Json.toJson(model.whoIsYourAccountant).asOpt[JsObject]
      ).flatten.fold(Json.obj()) {
        _ ++ _
      }
  }

  implicit def default(businessActivities: Option[BusinessActivities]): BusinessActivities =
    businessActivities.getOrElse(BusinessActivities())
}
