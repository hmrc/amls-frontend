package models.renewal
import cats.Functor
import cats.implicits._
import models.SubscriptionRequest

object Conversions {

  implicit class SubscriptionConversions(request: SubscriptionRequest) {

    def withRenewalData(renewal: Renewal): SubscriptionRequest = {
      val newSection = request.businessActivitiesSection match {
        case Some(ba) => Some(ba.copy(
          expectedAMLSTurnover = renewal.turnover contramap AMLSTurnover.convert,
          expectedBusinessTurnover = renewal.businessTurnover contramap BusinessTurnover.convert,
          involvedInOther = renewal.involvedInOtherActivities contramap InvolvedInOther.convert,
          customersOutsideUK = renewal.customersOutsideUK contramap CustomersOutsideUK.convert
        ))
        case _ => throw new Exception("[Conversions] Trying to process data for renewal, but no business activities data was found")
      }

      request.copy(businessActivitiesSection = newSection)
    }

  }

  implicit class ConversionSyntax[A](target: Option[A])(implicit fnc: Functor[Option]) {
    def contramap[B](fn: A => B) = fnc.lift(fn)(target)
  }

}
