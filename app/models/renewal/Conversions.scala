package models.renewal
import cats.Functor
import cats.implicits._
import models.SubscriptionRequest

object Conversions {

  implicit class SubscriptionConversions(request: SubscriptionRequest) {

    def fromRenewal(renewal: Renewal): SubscriptionRequest = {
      val newSection = request.businessActivitiesSection match {
        case Some(ba) => Some(ba.copy(
          expectedAMLSTurnover = renewal.turnover mappedBy AMLSTurnover.convert,
          expectedBusinessTurnover = renewal.businessTurnover mappedBy BusinessTurnover.convert,
          involvedInOther = renewal.involvedInOtherActivities mappedBy InvolvedInOther.convert,
          customersOutsideUK = renewal.customersOutsideUK mappedBy CustomersOutsideUK.convert
        ))
      }

      request.copy(businessActivitiesSection = newSection)
    }

  }

  implicit class ConversionSyntax[A](target: Option[A])(implicit fnc: Functor[Option]) {
    def mappedBy[B](fn: A => B) = fnc.lift(fn)(target)
  }

}
