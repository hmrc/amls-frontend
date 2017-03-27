package models.renewal
import cats.Functor
import cats.implicits._
import models.SubscriptionRequest
import models.renewal.AMLSTurnover.convert

object Conversions {

  implicit class SubscriptionConversions(request: SubscriptionRequest) {

    def fromRenewal(renewal: Renewal): SubscriptionRequest = {
      val newSection = request.businessActivitiesSection match {
        case Some(ba) => Some(ba.copy(
          expectedAMLSTurnover = Functor[Option].lift(convert)(renewal.turnover)
        ))
      }

      request.copy(businessActivitiesSection = newSection)
    }

  }

}
