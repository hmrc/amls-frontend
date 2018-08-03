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

package models.renewal
import cats.Functor
import cats.implicits._
import models.SubscriptionRequest

object Conversions {

  implicit class SubscriptionConversions(request: SubscriptionRequest) {

    def withRenewalData(renewal: Renewal): SubscriptionRequest = {

      val baSection = request.businessActivitiesSection match {
        case Some(ba) => Some(ba.copy(
          expectedAMLSTurnover = renewal.turnover contramap AMLSTurnover.convert,
          expectedBusinessTurnover = renewal.businessTurnover contramap BusinessTurnover.convert,
          involvedInOther = renewal.involvedInOtherActivities contramap InvolvedInOther.convert,
          customersOutsideUK = renewal.customersOutsideUK contramap CustomersOutsideUK.convert
        ))
        case _ => throw new Exception("[Conversions] Trying to process data for renewal, but no business activities data was found")
      }

      val msbSection = request.msbSection flatMap { msb =>
        Some(msb.copy(
          throughput = renewal.totalThroughput contramap TotalThroughput.convert,
          transactionsInNext12Months = renewal.transactionsInLast12Months contramap TransactionsInLast12Months.convert,
          sendTheLargestAmountsOfMoney = renewal.sendTheLargestAmountsOfMoney contramap SendTheLargestAmountsOfMoney.convert,
          mostTransactions = renewal.mostTransactions contramap MostTransactions.convert,
          ceTransactionsInNext12Months = renewal.ceTransactionsInLast12Months contramap CETransactionsInLast12Months.convert,
          whichCurrencies = renewal.whichCurrencies contramap WhichCurrencies.convert,
          fxTransactionsInNext12Months = renewal.fxTransactionsInLast12Months contramap FXTransactionsInLast12Months.convert
        ))
      }

      val hvdSection = request.hvdSection flatMap { hvd =>
        Some(hvd.copy(
          percentageOfCashPaymentOver15000 = renewal.percentageOfCashPaymentOver15000 contramap PercentageOfCashPaymentOver15000.convert,
          receiveCashPayments = Some((renewal.receiveCashPayments contramap ReceiveCashPayments.convert).isDefined),
          cashPaymentMethods = renewal.receiveCashPayments flatMap ReceiveCashPayments.convert
        ))
      }
      request.copy(businessActivitiesSection = baSection, msbSection = msbSection, hvdSection = hvdSection)
    }
  }

  implicit class ConversionSyntax[A](target: Option[A])(implicit fnc: Functor[Option]) {
    def contramap[B](fn: A => B) = fnc.lift(fn)(target)
  }

}
