package models.businessactivities

import controllers.businessactivities.AccountantForAMLSRegulationsController
import models.businessactivities.AccountantForAMLSRegulations

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
                               riskAssessmentPolicy: Option[RiskAssessmentPolicy] = None
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

  def riskAssessmentspolicy(p: RiskAssessmentPolicy) : BusinessActivities =
    this.copy(riskAssessmentPolicy = Some(p))

}

object BusinessActivities {

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
    __.read[Option[RiskAssessmentPolicy]]
    )(BusinessActivities.apply _)

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
      Json.toJson(model.riskAssessmentPolicy).asOpt[JsObject]
    ).flatten.fold(Json.obj()){
     _ ++ _
    }
  }

  implicit def default(businessActivities: Option[BusinessActivities]): BusinessActivities =
    businessActivities.getOrElse(BusinessActivities())
}