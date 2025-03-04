/*
 * Copyright 2024 HM Revenue & Customs
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

package models.renewal

import models.Country
import play.api.libs.json.{Json, OWrites, Reads}

case class Renewal(
  involvedInOtherActivities: Option[InvolvedInOther] = None,
  businessTurnover: Option[BusinessTurnover] = None,
  turnover: Option[AMLSTurnover] = None,
  ampTurnover: Option[AMPTurnover] = None,
  customersOutsideIsUK: Option[CustomersOutsideIsUK] = None,
  customersOutsideUK: Option[CustomersOutsideUK] = None,
  percentageOfCashPaymentOver15000: Option[PercentageOfCashPaymentOver15000] = None,
  receiveCashPayments: Option[CashPayments] = None,
  totalThroughput: Option[TotalThroughput] = None,
  whichCurrencies: Option[WhichCurrencies] = None,
  transactionsInLast12Months: Option[TransactionsInLast12Months] = None,
  sendTheLargestAmountsOfMoney: Option[SendTheLargestAmountsOfMoney] = None,
  mostTransactions: Option[MostTransactions] = None,
  ceTransactionsInLast12Months: Option[CETransactionsInLast12Months] = None,
  fxTransactionsInLast12Months: Option[FXTransactionsInLast12Months] = None,
  hasChanged: Boolean = false,
  sendMoneyToOtherCountry: Option[SendMoneyToOtherCountry] = None,
  hasAccepted: Boolean = true
) {

  def involvedInOtherActivities(model: InvolvedInOther): Renewal =
    this.copy(
      involvedInOtherActivities = Some(model),
      hasChanged = hasChanged || !this.involvedInOtherActivities.contains(model),
      hasAccepted = hasAccepted && this.involvedInOtherActivities.contains(model)
    )

  def businessTurnover(model: BusinessTurnover): Renewal =
    this.copy(
      businessTurnover = Some(model),
      hasChanged = hasChanged || !this.businessTurnover.contains(model),
      hasAccepted = hasAccepted && this.businessTurnover.contains(model)
    )

  def resetBusinessTurnover: Renewal =
    this.copy(businessTurnover = None, hasChanged = hasChanged || !this.businessTurnover.isEmpty)

  def turnover(model: AMLSTurnover): Renewal =
    this.copy(
      turnover = Some(model),
      hasChanged = hasChanged || !this.turnover.contains(model),
      hasAccepted = hasAccepted && this.turnover.contains(model)
    )

  def ampTurnover(model: AMPTurnover): Renewal =
    this.copy(
      ampTurnover = Some(model),
      hasChanged = hasChanged || !this.ampTurnover.contains(model),
      hasAccepted = hasAccepted && this.ampTurnover.contains(model)
    )

  def customersOutsideIsUK(model: CustomersOutsideIsUK): Renewal =
    this.copy(
      customersOutsideIsUK = Some(model),
      hasChanged = hasChanged || !this.customersOutsideIsUK.contains(model),
      hasAccepted = hasAccepted && this.customersOutsideIsUK.contains(model)
    )

  def customersOutsideUK(model: CustomersOutsideUK): Renewal =
    this.copy(
      customersOutsideUK = Some(model),
      hasChanged = hasChanged || !this.customersOutsideUK.contains(model),
      hasAccepted = hasAccepted && this.customersOutsideUK.contains(model)
    )

  def percentageOfCashPaymentOver15000(v: PercentageOfCashPaymentOver15000): Renewal =
    this.copy(percentageOfCashPaymentOver15000 = Some(v))

  def receiveCashPayments(p: CashPayments): Renewal =
    this.copy(
      receiveCashPayments = Some(p),
      hasChanged = hasChanged || !this.receiveCashPayments.contains(p),
      hasAccepted = hasAccepted && this.receiveCashPayments.contains(p)
    )

  def totalThroughput(model: TotalThroughput): Renewal =
    this.copy(
      totalThroughput = Some(model),
      hasChanged = hasChanged || !this.totalThroughput.contains(model),
      hasAccepted = hasAccepted && this.totalThroughput.contains(model)
    )

  def whichCurrencies(model: WhichCurrencies): Renewal =
    this.copy(
      whichCurrencies = Some(model),
      hasChanged = hasChanged || !this.whichCurrencies.contains(model),
      hasAccepted = hasAccepted && this.whichCurrencies.contains(model)
    )

  def transactionsInLast12Months(model: TransactionsInLast12Months): Renewal =
    this.copy(
      transactionsInLast12Months = Some(model),
      hasChanged = hasChanged || !this.transactionsInLast12Months.contains(model),
      hasAccepted = hasAccepted && this.transactionsInLast12Months.contains(model)
    )

  def sendMoneyToOtherCountry(model: SendMoneyToOtherCountry): Renewal =
    this.copy(
      sendMoneyToOtherCountry = Some(model),
      hasChanged = hasChanged || !this.sendMoneyToOtherCountry.contains(model),
      hasAccepted = hasAccepted && this.sendMoneyToOtherCountry.contains(model)
    )

  def sendTheLargestAmountsOfMoney(p: SendTheLargestAmountsOfMoney): Renewal =
    this.copy(
      sendTheLargestAmountsOfMoney = Some(p),
      hasChanged = hasChanged || !this.sendTheLargestAmountsOfMoney.contains(p),
      hasAccepted = hasAccepted && this.sendTheLargestAmountsOfMoney.contains(p)
    )

  def ceTransactionsInLast12Months(p: CETransactionsInLast12Months): Renewal =
    this.copy(
      ceTransactionsInLast12Months = Some(p),
      hasChanged = hasChanged || !this.ceTransactionsInLast12Months.contains(p),
      hasAccepted = hasAccepted && this.ceTransactionsInLast12Months.contains(p)
    )

  def fxTransactionsInLast12Months(p: FXTransactionsInLast12Months): Renewal =
    this.copy(
      fxTransactionsInLast12Months = Some(p),
      hasChanged = hasChanged || !this.fxTransactionsInLast12Months.contains(p),
      hasAccepted = hasAccepted && this.fxTransactionsInLast12Months.contains(p)
    )

  def mostTransactions(model: MostTransactions): Renewal =
    this.copy(
      mostTransactions = Some(model),
      hasChanged = hasChanged || !this.mostTransactions.contains(model),
      hasAccepted = hasAccepted && this.mostTransactions.contains(model)
    )

  private def basicPropertyRule: Boolean = involvedInOtherActivities.isDefined && turnover.isDefined

  private def involvedInOtherRule: Boolean = this match {
    case r if r.involvedInOtherActivities.exists(_.isInstanceOf[InvolvedInOtherYes]) && r.businessTurnover.isDefined =>
      true
    case r if r.involvedInOtherActivities.contains(InvolvedInOtherNo) && r.businessTurnover.isEmpty                  => true
    case _                                                                                                           => false
  }

  def currencyExchangeRule: Boolean = whichCurrencies.isDefined && ceTransactionsInLast12Months.isDefined

  def moneyTransmitterRule: Boolean = this match {
    case r
        if r.sendMoneyToOtherCountry.exists(_.money == true) &&
          r.transactionsInLast12Months.isDefined &&
          r.mostTransactions.isDefined &&
          r.sendTheLargestAmountsOfMoney.isDefined =>
      true

    case r
        if (r.sendMoneyToOtherCountry.isEmpty || r.sendMoneyToOtherCountry.exists(_.money == false)) &&
          r.transactionsInLast12Months.isDefined &&
          r.mostTransactions.isEmpty =>
      true

    case _ => false
  }

  def aspRule: Boolean = this match {
    case Renewal(_, _, _, _, Some(CustomersOutsideIsUK(true)), Some(_), _, _, _, _, _, _, _, _, _, _, _, _) => true
    case Renewal(_, _, _, _, Some(CustomersOutsideIsUK(false)), _, _, _, _, _, _, _, _, _, _, _, _, _)      => true
    case _                                                                                                  => false
  }

  private def receiveCashPaymentsRule: Boolean = this match {
    case Renewal(
          _,
          _,
          _,
          _,
          _,
          _,
          _,
          Some(CashPayments(CashPaymentsCustomerNotMet(true), Some(_))),
          _,
          _,
          _,
          _,
          _,
          _,
          _,
          _,
          _,
          _
        ) =>
      true
    case Renewal(
          _,
          _,
          _,
          _,
          _,
          _,
          _,
          Some(CashPayments(CashPaymentsCustomerNotMet(false), None)),
          _,
          _,
          _,
          _,
          _,
          _,
          _,
          _,
          _,
          _
        ) =>
      true
    case _ => false
  }

  def hvdRule: Boolean = Seq(
    percentageOfCashPaymentOver15000.isDefined,
    receiveCashPaymentsRule,
    aspRule
  ).forall(identity)

  def standardRule: Boolean = Seq(
    basicPropertyRule,
    involvedInOtherRule,
    hasAccepted
  ).forall(identity)
}

object Renewal {
  val key        = "renewal"
  val sectionKey = "renewal"

  implicit val format: OWrites[Renewal] = Json.writes[Renewal]

  implicit val jsonReads: Reads[Renewal] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._

    (
      (__ \ "involvedInOtherActivities").readNullable[InvolvedInOther] and
        (__ \ "businessTurnover").readNullable[BusinessTurnover] and
        (__ \ "turnover").readNullable[AMLSTurnover] and
        (__ \ "ampTurnover").readNullable[AMPTurnover] and
        ((__ \ "customersOutsideUK" \ "isOutside").read[Boolean].map(c => Option(CustomersOutsideIsUK(c))) orElse
          (__ \ "customersOutsideIsUK").readNullable[CustomersOutsideIsUK]) and
        ((__ \ "customersOutsideUK" \ "countries")
          .readNullable[Seq[Country]]
          .map(c => Option(CustomersOutsideUK(c))) orElse
          (__ \ "customersOutsideUK").readNullable[CustomersOutsideUK]) and
        (__ \ "percentageOfCashPaymentOver15000").readNullable[PercentageOfCashPaymentOver15000] and
        (__ \ "receiveCashPayments").readNullable[CashPayments] and
        (__ \ "totalThroughput").readNullable[TotalThroughput] and
        (__ \ "whichCurrencies").readNullable[WhichCurrencies] and
        (__ \ "transactionsInLast12Months").readNullable[TransactionsInLast12Months] and
        (__ \ "sendTheLargestAmountsOfMoney").readNullable[SendTheLargestAmountsOfMoney] and
        (__ \ "mostTransactions").readNullable[MostTransactions] and
        (__ \ "ceTransactionsInLast12Months").readNullable[CETransactionsInLast12Months] and
        (__ \ "fxTransactionsInLast12Months").readNullable[FXTransactionsInLast12Months] and
        (__ \ "hasChanged").read[Boolean] and
        (__ \ "sendMoneyToOtherCountry").readNullable[SendMoneyToOtherCountry] and
        (__ \ "hasAccepted").read[Boolean]
    ).apply(Renewal.apply _)
  }

  implicit def default(renewal: Option[Renewal]): Renewal =
    renewal.getOrElse(Renewal())
}
