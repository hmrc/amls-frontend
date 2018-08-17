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

package models.moneyservicebusiness

import config.ApplicationConfig
import models.businessmatching.{BusinessMatching, CurrencyExchange, ForeignExchange, TransmittingMoney}
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import play.api.libs.json._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.ControllerHelper

case class MoneyServiceBusiness(
                                 throughput: Option[ExpectedThroughput] = None,
                                 businessUseAnIPSP: Option[BusinessUseAnIPSP] = None,
                                 identifyLinkedTransactions: Option[IdentifyLinkedTransactions] = None,
                                 whichCurrencies: Option[WhichCurrencies] = None,
                                 sendMoneyToOtherCountry: Option[SendMoneyToOtherCountry] = None,
                                 fundsTransfer: Option[FundsTransfer] = None,
                                 branchesOrAgents: Option[BranchesOrAgents] = None,
                                 sendTheLargestAmountsOfMoney: Option[SendTheLargestAmountsOfMoney] = None,
                                 mostTransactions: Option[MostTransactions] = None,
                                 transactionsInNext12Months: Option[TransactionsInNext12Months] = None,
                                 ceTransactionsInNext12Months: Option[CETransactionsInNext12Months] = None,
                                 fxTransactionsInNext12Months: Option[FXTransactionsInNext12Months] = None,
                                 hasChanged: Boolean = false,
                                 hasAccepted: Boolean = false
                               ) {

  def throughput(p: ExpectedThroughput): MoneyServiceBusiness =
    this.copy(throughput = Some(p), hasChanged = hasChanged || !this.throughput.contains(p), hasAccepted = this.throughput.contains(p))

  def whichCurrencies(p: WhichCurrencies): MoneyServiceBusiness =
    this.copy(whichCurrencies = Some(p), hasChanged = hasChanged || !this.whichCurrencies.contains(p), hasAccepted = this.whichCurrencies.contains(p))

  def businessUseAnIPSP(p: BusinessUseAnIPSP): MoneyServiceBusiness =
    this.copy(businessUseAnIPSP = Some(p), hasChanged = hasChanged || !this.businessUseAnIPSP.contains(p), hasAccepted = this.businessUseAnIPSP.contains(p))

  def identifyLinkedTransactions(p: IdentifyLinkedTransactions): MoneyServiceBusiness =
    this.copy(identifyLinkedTransactions = Some(p), hasChanged = hasChanged || !this.identifyLinkedTransactions.contains(p), hasAccepted = this.identifyLinkedTransactions.contains(p))

  def fundsTransfer(p: FundsTransfer): MoneyServiceBusiness =
    this.copy(fundsTransfer = Some(p), hasChanged = hasChanged || !this.fundsTransfer.contains(p), hasAccepted = this.fundsTransfer.contains(p))

  def branchesOrAgents(p: BranchesOrAgents): MoneyServiceBusiness =
    this.copy(branchesOrAgents = Some(p), hasChanged = hasChanged || !this.branchesOrAgents.contains(p), hasAccepted = this.branchesOrAgents.contains(p))

  def sendMoneyToOtherCountry(p: SendMoneyToOtherCountry): MoneyServiceBusiness =
    this.copy(sendMoneyToOtherCountry = Some(p), hasChanged = hasChanged || !this.sendMoneyToOtherCountry.contains(p), hasAccepted = this.sendMoneyToOtherCountry.contains(p))

  def sendTheLargestAmountsOfMoney(p: Option[SendTheLargestAmountsOfMoney]): MoneyServiceBusiness =
    this.copy(sendTheLargestAmountsOfMoney = p, hasChanged = hasChanged || !this.sendTheLargestAmountsOfMoney.equals(p),
      hasAccepted = this.sendTheLargestAmountsOfMoney.equals(p))

  def mostTransactions(p: Option[MostTransactions]): MoneyServiceBusiness =
    this.copy(mostTransactions = p, hasChanged = hasChanged || !this.mostTransactions.equals(p), hasAccepted = this.mostTransactions.equals(p))

  def transactionsInNext12Months(p: TransactionsInNext12Months): MoneyServiceBusiness =
    this.copy(transactionsInNext12Months = Some(p), hasChanged = hasChanged || !this.transactionsInNext12Months.contains(p), hasAccepted = this.transactionsInNext12Months.contains(p))

  def ceTransactionsInNext12Months(p: CETransactionsInNext12Months): MoneyServiceBusiness =
    this.copy(ceTransactionsInNext12Months = Some(p), hasChanged = hasChanged || !this.ceTransactionsInNext12Months.contains(p), hasAccepted = this.ceTransactionsInNext12Months.contains(p))

  def fxTransactionsInNext12Months(p: FXTransactionsInNext12Months): MoneyServiceBusiness =
    this.copy(fxTransactionsInNext12Months = Some(p), hasChanged = hasChanged || !this.fxTransactionsInNext12Months.contains(p), hasAccepted = this.fxTransactionsInNext12Months.contains(p))

  private def allComplete: Boolean =
    this.throughput.isDefined &&
      this.branchesOrAgents.isDefined &&
      this.identifyLinkedTransactions.isDefined

  private def mtComplete(mtFlag: Boolean): Boolean =
    if (mtFlag) {
        this.businessUseAnIPSP.isDefined &&
        this.fundsTransfer.isDefined &&
        this.transactionsInNext12Months.isDefined &&
        (
          (
            this.sendMoneyToOtherCountry.contains(SendMoneyToOtherCountry(true)) &&
              this.sendTheLargestAmountsOfMoney.isDefined &&
              this.mostTransactions.isDefined
            ) || this.sendMoneyToOtherCountry.contains(SendMoneyToOtherCountry(false))
          )
    } else {
      true
    }

  private def ceComplete(ceFlag: Boolean): Boolean =
    if (ceFlag) {
      this.ceTransactionsInNext12Months.isDefined && this.whichCurrencies.isDefined
    } else {
      true
    }

  private def fxComplete(fxFlag: Boolean): Boolean =
    if (fxFlag) {
      this.fxTransactionsInNext12Months.isDefined
    } else {
      true
    }

  def isComplete(mtFlag: Boolean, ceFlag: Boolean, fxFlag: Boolean): Boolean = if(ApplicationConfig.hasAcceptedToggle) {
    allComplete && mtComplete(mtFlag) && ceComplete(ceFlag) && fxComplete(fxFlag) && this.hasAccepted
  } else {
    allComplete && mtComplete(mtFlag) && ceComplete(ceFlag) && fxComplete(fxFlag)
  }
}

object MoneyServiceBusiness {

  val key = "msb"

  implicit val formatOption = Reads.optionWithNull[MoneyServiceBusiness]

  def section(implicit cache: CacheMap): Section = {
    val messageKey = key
    val notStarted = Section(messageKey, NotStarted, false, controllers.msb.routes.WhatYouNeedController.get())

    cache.getEntry[MoneyServiceBusiness](key).fold(notStarted) {
      model =>
        val msbService = ControllerHelper.getMsbServices(cache.getEntry[BusinessMatching](BusinessMatching.key)).getOrElse(Set.empty)
        if (model.isComplete(msbService.contains(TransmittingMoney), msbService.contains(CurrencyExchange), msbService.contains(ForeignExchange))) {
          Section(messageKey, Completed, model.hasChanged, controllers.msb.routes.SummaryController.get())
        } else {
          Section(messageKey, Started, model.hasChanged, controllers.msb.routes.WhatYouNeedController.get())
        }
    }
  }

  implicit val reads: Reads[MoneyServiceBusiness] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "throughput").readNullable[ExpectedThroughput] and
        (__ \ "businessUseAnIPSP").readNullable[BusinessUseAnIPSP] and
        (__ \ "identifyLinkedTransactions").readNullable[IdentifyLinkedTransactions] and
        (__ \ "whichCurrencies").readNullable[WhichCurrencies] and
        (__ \ "sendMoneyToOtherCountry").readNullable[SendMoneyToOtherCountry] and
        (__ \ "fundsTransfer").readNullable[FundsTransfer] and
        (__ \ "branchesOrAgents").readNullable[BranchesOrAgents] and
        (__ \ "sendTheLargestAmountsOfMoney").readNullable[SendTheLargestAmountsOfMoney] and
        (__ \ "mostTransactions").readNullable[MostTransactions] and
        (__ \ "transactionsInNext12Months").readNullable[TransactionsInNext12Months] and
        (__ \ "ceTransactionsInNext12Months").readNullable[CETransactionsInNext12Months] and
        (__ \ "fxTransactionsInNext12Months").readNullable[FXTransactionsInNext12Months] and
        (__ \ "hasChanged").readNullable[Boolean].map {
          _.getOrElse(false)
        } and
        (__ \ "hasAccepted").readNullable[Boolean].map {
          _.getOrElse(false)
        }
      ) apply MoneyServiceBusiness.apply _
  }

  implicit val writes: Writes[MoneyServiceBusiness] = Json.writes[MoneyServiceBusiness]

  implicit def default(value: Option[MoneyServiceBusiness]): MoneyServiceBusiness = {
    value.getOrElse(MoneyServiceBusiness())
  }
}


