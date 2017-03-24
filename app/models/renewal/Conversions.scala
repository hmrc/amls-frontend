package models.renewal

import models.SubscriptionRequest

object Conversions {

  implicit class SubscriptionConversions(request: SubscriptionRequest) {

    def asRenewal(renewal: Renewal): SubscriptionRequest = {
      val newSection = request.businessActivitiesSection match {
        case Some(ba) => Some(ba.copy(
          expectedAMLSTurnover = ???
        ))
      }

      request.copy(businessActivitiesSection = newSection)
    }

  }

}
