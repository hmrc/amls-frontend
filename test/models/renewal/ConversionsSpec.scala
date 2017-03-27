package models.renewal

import models.SubscriptionRequest
import models.businessactivities.BusinessActivities
import models.renewal.Conversions._
import org.scalatest.{MustMatchers, WordSpec}

class ConversionsSpec extends WordSpec with MustMatchers {

  trait Fixture {

    val businessActivities = BusinessActivities()

    val subscriptionRequest = SubscriptionRequest(None, None, None, None, None, None, Some(businessActivities), None, None, None, None, None, None)

  }

  "The renewal converter" must {

    "convert the AMLS expected turnover" in new Fixture {
      val turnover: AMLSTurnover = AMLSTurnover.First
      val renewal = Renewal(turnover = Some(turnover))

      val converted = subscriptionRequest.fromRenewal(renewal)

      converted.businessActivitiesSection.get.expectedAMLSTurnover mustBe Some(models.businessactivities.ExpectedAMLSTurnover.First)
    }

    "convert the business turnover" in new Fixture {
      val businessTurnover: BusinessTurnover = BusinessTurnover.Second
      val renewal = Renewal(businessTurnover = Some(businessTurnover))

      val converted = subscriptionRequest.fromRenewal(renewal)

      converted.businessActivitiesSection.get.expectedBusinessTurnover mustBe Some(models.businessactivities.ExpectedBusinessTurnover.Second)
    }

    "convert the 'involved in other businesses' model" in new Fixture {

      val model: InvolvedInOther = InvolvedInOtherYes("some other business")
      val renewal = Renewal(involvedInOtherActivities = Some(model))

      val converted = subscriptionRequest.fromRenewal(renewal)

      converted.businessActivitiesSection.get.involvedInOther mustBe Some(models.businessactivities.InvolvedInOtherYes("some other business"))
    }

  }

}
