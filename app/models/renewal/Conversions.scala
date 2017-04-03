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

      val msbSection = request.msbSection match {
        case Some(msb) => Some(msb.copy(
          throughput = renewal.msbThroughput contramap MsbThroughput.convert
        ))
        case _ => None
      }

      request.copy(businessActivitiesSection = baSection, msbSection = msbSection)
    }

  }

  implicit class ConversionSyntax[A](target: Option[A])(implicit fnc: Functor[Option]) {
    def contramap[B](fn: A => B) = fnc.lift(fn)(target)
  }

}
