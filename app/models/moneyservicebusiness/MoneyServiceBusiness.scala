package models.moneyservicebusiness

import models.Country
import models.registrationprogress.{Completed, NotStarted, Started, Section}
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap
import play.api.libs.json._

case class MoneyServiceBusiness(
                                 msbServices : Option[MsbServices] = None,
                                 throughput : Option[ExpectedThroughput] = None,
                                 businessUseAnIPSP: Option[BusinessUseAnIPSP] = None,
                                 identifyLinkedTransactions: Option[IdentifyLinkedTransactions] = None,
                                 whichCurrencies : Option[WhichCurrencies] = None,
                                 businessAppliedForPSRNumber: Option[BusinessAppliedForPSRNumber] = None,
                                 sendMoneyToOtherCountry: Option[SendMoneyToOtherCountry] = None,
                                 fundsTransfer : Option[FundsTransfer] = None,
                                 branchesOrAgents: Option[BranchesOrAgents] = None,
                                 sendTheLargestAmountsOfMoney: Option[SendTheLargestAmountsOfMoney] = None,
                                 mostTransactions: Option[MostTransactions] = None,
                                 transactionsInNext12Months: Option[TransactionsInNext12Months] = None,
                                 ceTransactionsInNext12Months: Option[CETransactionsInNext12Months] = None
                               ) {

  def msbServices(p: MsbServices): MoneyServiceBusiness =
    this.copy(msbServices = Some(p))

  def throughput(p: ExpectedThroughput): MoneyServiceBusiness =
    this.copy(throughput = Some(p))

  def whichCurrencies(p: WhichCurrencies): MoneyServiceBusiness =
    this.copy(whichCurrencies = Some(p))

  def businessUseAnIPSP(p: BusinessUseAnIPSP): MoneyServiceBusiness =
    this.copy(businessUseAnIPSP = Some(p))

  def identifyLinkedTransactions(p: IdentifyLinkedTransactions): MoneyServiceBusiness =
    this.copy(identifyLinkedTransactions = Some(p))

  def fundsTransfer(p: FundsTransfer): MoneyServiceBusiness =
    this.copy(fundsTransfer = Some(p))

  def branchesOrAgents(p: BranchesOrAgents): MoneyServiceBusiness =
    this.copy(branchesOrAgents = Some(p))

  def businessAppliedForPSRNumber(p: BusinessAppliedForPSRNumber): MoneyServiceBusiness =
    this.copy(businessAppliedForPSRNumber = Some(p))

  def sendMoneyToOtherCountry(p: SendMoneyToOtherCountry): MoneyServiceBusiness =
    this.copy(sendMoneyToOtherCountry = Some(p))

  def sendTheLargestAmountsOfMoney(p: SendTheLargestAmountsOfMoney): MoneyServiceBusiness =
    this.copy(sendTheLargestAmountsOfMoney = Some(p))

  def mostTransactions(p: MostTransactions): MoneyServiceBusiness =
    this.copy(mostTransactions = Some(p))

   def transactionsInNext12Months(p: TransactionsInNext12Months): MoneyServiceBusiness =
    this.copy(transactionsInNext12Months = Some(p))

  def ceTransactionsInNext12Months(p: CETransactionsInNext12Months): MoneyServiceBusiness =
    this.copy(ceTransactionsInNext12Months = Some(p))

  private def allComplete: Boolean =
    this.msbServices.isDefined &&
    this.throughput.isDefined &&
    this.branchesOrAgents.isDefined &&
    this.identifyLinkedTransactions.isDefined

  private def mtComplete: Boolean =
    if (this.msbServices.exists(_.services contains TransmittingMoney)) {
      this.businessAppliedForPSRNumber.isDefined &&
        this.businessUseAnIPSP.isDefined &&
        this.fundsTransfer.isDefined &&
        this.transactionsInNext12Months.isDefined &&
        (
          (
            this.sendMoneyToOtherCountry.contains(SendMoneyToOtherCountry(true)) &&
              this.sendTheLargestAmountsOfMoney.isDefined &&
              this.mostTransactions.isDefined
            ) ||
            this.sendMoneyToOtherCountry.contains(SendMoneyToOtherCountry(false))
          )
    } else {
      true
    }

  private def ceComplete: Boolean =
    if (this.msbServices.exists(_.services contains CurrencyExchange)) {
      this.ceTransactionsInNext12Months.isDefined &&
      this.whichCurrencies.isDefined
    } else {
      true
    }

  def isComplete: Boolean =
    allComplete && mtComplete && ceComplete
}

object MoneyServiceBusiness {

  val key = "msb"

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


