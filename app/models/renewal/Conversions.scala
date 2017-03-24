package models.renewal

import models.SubscriptionRequest

object Conversions {

  implicit class SubscriptionConversions(request: SubscriptionRequest) {

    def asRenewal(renewal: Renewal): SubscriptionRequest = request

  }

}
