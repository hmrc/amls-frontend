package models.renewal
import cats.Functor
import cats.implicits._
import models.SubscriptionRequest

object Conversions {

  implicit class SubscriptionConversions(request: SubscriptionRequest) {

    def fromRenewal(renewal: Renewal): SubscriptionRequest = {
      val newSection = request.businessActivitiesSection match {
        case Some(ba) => Some(ba.copy(
          expectedAMLSTurnover = Functor[Option].lift(AMLSTurnover.convert)(renewal.turnover),
          expectedBusinessTurnover = Functor[Option].lift(BusinessTurnover.convert)(renewal.businessTurnover),
          involvedInOther = Functor[Option].lift(InvolvedInOther.convert)(renewal.involvedInOtherActivities),
          customersOutsideUK = Functor[Option].lift(CustomersOutsideUK.convert)(renewal.customersOutsideUK)
        ))
      }

      request.copy(businessActivitiesSection = newSection)
    }

  }

}
