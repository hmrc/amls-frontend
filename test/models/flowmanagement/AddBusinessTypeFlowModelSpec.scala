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

package models.flowmanagement

import models.businessmatching.BusinessActivity._
import models.businessmatching.BusinessMatchingMsbService._
import models.businessmatching._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.Helpers.stubMessages

class AddBusinessTypeFlowModelSpec extends PlaySpec {

  "AddBusinessTypeFlowModel" must {

    "activity setter" must {
      "set hasAccepted to true when value matches existing" in {
        val model = AddBusinessTypeFlowModel(activity = Some(AccountancyServices), hasAccepted = true)
        model.activity(AccountancyServices).hasAccepted mustBe true
      }

      "set hasAccepted to false when value does not match existing" in {
        val model = AddBusinessTypeFlowModel(activity = Some(AccountancyServices), hasAccepted = true)
        model.activity(ArtMarketParticipant).hasAccepted mustBe false
      }
    }

    "businessAppliedForPSRNumber setter" must {
      "set hasAccepted to true when value matches existing" in {
        val psr   = BusinessAppliedForPSRNumberYes("123456")
        val model = AddBusinessTypeFlowModel(businessAppliedForPSRNumber = Some(psr), hasAccepted = true)
        model.businessAppliedForPSRNumber(psr).hasAccepted mustBe true
      }

      "set hasAccepted to false when value does not match" in {
        val model = AddBusinessTypeFlowModel(hasAccepted = true)
        model.businessAppliedForPSRNumber(BusinessAppliedForPSRNumberNo).hasAccepted mustBe false
      }
    }

    "msbServices setter" must {
      "set hasAccepted to true when value matches existing" in {
        val services = BusinessMatchingMsbServices(Set(TransmittingMoney))
        val model    = AddBusinessTypeFlowModel(subSectors = Some(services), hasAccepted = true)
        model.msbServices(services).hasAccepted mustBe true
      }

      "set hasAccepted to false when value does not match" in {
        val model = AddBusinessTypeFlowModel(hasAccepted = true)
        model.msbServices(BusinessMatchingMsbServices(Set(ChequeCashingNotScrapMetal))).hasAccepted mustBe false
      }

      "retain businessAppliedForPSRNumber when TransmittingMoney is in new subSectors" in {
        val psr      = BusinessAppliedForPSRNumberYes("123456")
        val services = BusinessMatchingMsbServices(Set(TransmittingMoney))
        val model    = AddBusinessTypeFlowModel(businessAppliedForPSRNumber = Some(psr))
        model.msbServices(services).businessAppliedForPSRNumber mustBe Some(psr)
      }

      "clear businessAppliedForPSRNumber when TransmittingMoney is not in new subSectors" in {
        val psr      = BusinessAppliedForPSRNumberYes("123456")
        val services = BusinessMatchingMsbServices(Set(ChequeCashingNotScrapMetal))
        val model    = AddBusinessTypeFlowModel(businessAppliedForPSRNumber = Some(psr))
        model.msbServices(services).businessAppliedForPSRNumber mustBe None
      }
    }

    "isComplete" must {
      "return true for MoneyServiceBusiness with PSR number, addMoreActivities and hasAccepted" in {
        val model = AddBusinessTypeFlowModel(
          activity = Some(MoneyServiceBusiness),
          addMoreActivities = Some(true),
          hasAccepted = true,
          businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("123456"))
        )
        model.isComplete mustBe true
      }

      "return true for non-MSB activity with addMoreActivities and hasAccepted" in {
        val model = AddBusinessTypeFlowModel(
          activity = Some(AccountancyServices),
          addMoreActivities = Some(true),
          hasAccepted = true
        )
        model.isComplete mustBe true
      }

      "return false when hasAccepted is false" in {
        val model = AddBusinessTypeFlowModel(
          activity = Some(AccountancyServices),
          addMoreActivities = Some(true),
          hasAccepted = false
        )
        model.isComplete mustBe false
      }

      "return false when model is empty" in {
        AddBusinessTypeFlowModel().isComplete mustBe false
      }
    }

    "informationRequired" must {
      "return false for BillPaymentServices" in {
        AddBusinessTypeFlowModel(activity = Some(BillPaymentServices)).informationRequired mustBe false
      }

      "return false for TelephonePaymentService" in {
        AddBusinessTypeFlowModel(activity = Some(TelephonePaymentService)).informationRequired mustBe false
      }

      "return true for other activities" in {
        AddBusinessTypeFlowModel(activity = Some(AccountancyServices)).informationRequired mustBe true
      }

      "return false when activity is None" in {
        AddBusinessTypeFlowModel().informationRequired mustBe false
      }
    }

    "activityName" must {
      "return the message for the activity" in {
        implicit val messages = stubMessages()
        val model             = AddBusinessTypeFlowModel(activity = Some(AccountancyServices))
        model.activityName mustBe defined
      }

      "return None when activity is not set" in {
        implicit val messages = stubMessages()
        AddBusinessTypeFlowModel().activityName mustBe None
      }
    }

    "JSON format" must {
      "serialise and deserialise correctly" in {
        val model = AddBusinessTypeFlowModel(
          activity = Some(AccountancyServices),
          addMoreActivities = Some(true),
          hasChanged = true,
          hasAccepted = true
        )
        Json.toJson(model).as[AddBusinessTypeFlowModel] mustEqual model
      }
    }
  }
}
