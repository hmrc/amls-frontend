/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services

import models.businessmatching.BusinessActivity
import models.businessmatching.BusinessActivity.{AccountancyServices, MoneyServiceBusiness, BillPaymentServices, EstateAgentBusinessService, HighValueDealing, TelephonePaymentService, TrustAndCompanyServices}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}

class ActivityServiceSpec extends WordSpec with MustMatchers with OptionValues {

  val activityService = new ActivityService()

  "getActivityValues is called" must {
      "have no existing services" when {
        "status is pre-submission" when {
          "activities have not yet been selected" in {
            activityService.getActivityValues(true, None) mustBe empty
          }

          "activities have already been selected" in {
            activityService.getActivityValues(true, Some(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))) mustBe empty
          }
        }
      }

      "return values for services excluding those provided" when {
        "status is post-submission" when {
          val activities: Set[BusinessActivity] = Set(
            AccountancyServices,
            BillPaymentServices,
            EstateAgentBusinessService,
            HighValueDealing,
            MoneyServiceBusiness,
            TrustAndCompanyServices,
            TelephonePaymentService
          )

          activities.foreach { act =>
            s"$act is contained in existing activities" in {
              val activityData: Set[BusinessActivity] = Set(act)
              activityService.getActivityValues(false, Some(activityData)) mustBe Seq(act)
            }
          }
        }
      }
    }
}
