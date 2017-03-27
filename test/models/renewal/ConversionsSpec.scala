package models.renewal

import models.SubscriptionRequest
import models.businessactivities.{BusinessActivities, ExpectedAMLSTurnover}
import models.renewal.AMLSTurnover.First
import org.scalatest.{MustMatchers, WordSpec}
import models.renewal.Conversions._

class ConversionsSpec extends WordSpec with MustMatchers {

  trait Fixture {

    val businessActivities = BusinessActivities()

    val subscriptionRequest = SubscriptionRequest(None, None, None, None, None, None, Some(businessActivities), None, None, None, None, None, None)

  }

  "The renewal converter" must {

    "convert the AMLS expected turnover" in new Fixture {

      val turnover: AMLSTurnover = First
      val renewal = Renewal(turnover = Some(turnover))

      val converted = subscriptionRequest.fromRenewal(renewal)

      converted.businessActivitiesSection.get.expectedAMLSTurnover mustBe Some(models.businessactivities.ExpectedAMLSTurnover.First)

    }

  }

}
