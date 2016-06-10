package models.moneyservicebusiness

import models.registrationprogress.{Completed, NotStarted, Started, Section}
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap
import play.api.libs.json._

case class MoneyServiceBusiness(
                                 msbServices : Option[MsbServices] = None,
                                 throughput : Option[ExpectedThroughput] = None,
                                 businessUseAnIPSP: Option[BusinessUseAnIPSP] = None,
                                 identifyLinkedTransactions: Option[IdentifyLinkedTransactions] = None,
                                 businessAppliedForPSRNumber: Option[BusinessAppliedForPSRNumber] = None,
                                 sendMoneyToOtherCountry: Option[SendMoneyToOtherCountry] = None,
                                 fundsTransfer : Option[FundsTransfer] = None
                                 branchesOrAgents: Option[BranchesOrAgents] = None
                               ) {

  def msbServices(p: MsbServices): MoneyServiceBusiness =
    this.copy(msbServices = Some(p))

  def throughput(p: ExpectedThroughput): MoneyServiceBusiness =
    this.copy(throughput = Some(p))

  def businessUseAnIPSP(p: BusinessUseAnIPSP): MoneyServiceBusiness =
    this.copy(businessUseAnIPSP = Some(p))

  def identifyLinkedTransactions(p: IdentifyLinkedTransactions): MoneyServiceBusiness =
    this.copy(identifyLinkedTransactions = Some(p))
  def fundsTransfer(p: FundsTransfer): MoneyServiceBusiness =
    this.copy(fundsTransfer = Some(p))

  def businessAppliedForPSRNumber(p: BusinessAppliedForPSRNumber): MoneyServiceBusiness =
    this.copy(businessAppliedForPSRNumber = Some(p))

  def sendMoneyToOtherCountry(p: SendMoneyToOtherCountry): MoneyServiceBusiness =
    this.copy(sendMoneyToOtherCountry = Some(p))

  def branchesOrAgents(p: BranchesOrAgents): MoneyServiceBusiness =
    this.copy(branchesOrAgents = Some(p))

  // TODO: Apply the actual logic here when we have the complete model
  def isComplete: Boolean = this match {
    case m if m.productIterator.forall {
        case Some(_) => true
        case None => false
      } => true
    case _ => false
  }
}

object MoneyServiceBusiness {

  val key = "msb"

  implicit val mongoKey = new MongoKey[MoneyServiceBusiness] {
    def apply() = key
  }

  def section(implicit cache: CacheMap): Section = {

    val messageKey = key

    val notStarted = Section(messageKey, NotStarted, controllers.msb.routes.WhatYouNeedController.get())
    cache.getEntry[MoneyServiceBusiness](key).fold(notStarted) {
      model =>
        if (model.isComplete) {
          Section(messageKey, Completed, controllers.msb.routes.SummaryController.get())
        } else {
          Section(messageKey, Started, controllers.msb.routes.WhatYouNeedController.get())
        }
    }
  }

  implicit val format =  Json.format[MoneyServiceBusiness]

  implicit def default(value : Option[MoneyServiceBusiness]) :  MoneyServiceBusiness = {
    value.getOrElse(MoneyServiceBusiness())
  }
}


