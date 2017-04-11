package models.renewal
import cats.Functor
import cats.implicits._
import models.SubscriptionRequest
import models.moneyservicebusiness.WhichCurrencies

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
          throughput = renewal.msbThroughput contramap MsbThroughput.convert,
          transactionsInNext12Months = renewal.msbTransfers contramap MsbMoneyTransfers.convert,
          sendTheLargestAmountsOfMoney = renewal.sendTheLargestAmountsOfMoney contramap MsbSendTheLargestAmountsOfMoney.convert,
          mostTransactions = renewal.mostTransactions contramap MsbMostTransactions.convert,
          ceTransactionsInNext12Months = renewal.ceTransactions contramap CETransactions.convert,
          whichCurrencies = renewal.msbWhichCurrencies contramap MsbWhichCurrencies.convert

        ))
      }

      val hvdSection = request.hvdSection flatMap { hvd =>
        Some(hvd.copy(
          percentageOfCashPaymentOver15000 = renewal.percentageOfCashPaymentOver15000 contramap PercentageOfCashPaymentOver15000.convert,
          receiveCashPayments = renewal.receiveCashPayments contramap ReceiveCashPayments.convert
        ))
      }
      request.copy(businessActivitiesSection = baSection, msbSection = msbSection, hvdSection = hvdSection)
    }

  }

  implicit class ConversionSyntax[A](target: Option[A])(implicit fnc: Functor[Option]) {
    def contramap[B](fn: A => B) = fnc.lift(fn)(target)
  }

}
