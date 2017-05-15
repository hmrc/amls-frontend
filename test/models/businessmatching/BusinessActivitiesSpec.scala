/*
 * Copyright 2017 HM Revenue & Customs
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

package models.businessmatching

import org.scalatest.mock.MockitoSugar
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.i18n.Messages
import play.api.libs.json._
import utils.GenericTestHelper


class BusinessActivitiesSpec extends GenericTestHelper with MockitoSugar {
  import jto.validation.forms.Rules._

  "BusinessActivitiesSpec" must {
    "successfully validate" when {
      "a few check boxes are selected" in {
        val model1 = Map("businessActivities[]" -> Seq("03", "01", "02"))

        BusinessActivities.formReads.validate(model1) must
          be(Valid(BusinessActivities(Set(EstateAgentBusinessService, AccountancyServices, BillPaymentServices))))

        val model2 = Map("businessActivities[]" -> Seq("04", "05", "06"))

        BusinessActivities.formReads.validate(model2) must
          be(Valid(BusinessActivities(Set(HighValueDealing, MoneyServiceBusiness, TrustAndCompanyServices))))

        BusinessActivities.formReads.validate(Map("businessActivities[]" -> Seq("07"))) must
          be(Valid(BusinessActivities(Set(TelephonePaymentService))))
      }

      "residential business activity check box is selected" in {
        val model = Map("businessActivities" -> Seq("07"))

        BusinessActivities.formReads.validate(model) must
          be(Valid(BusinessActivities(Set(TelephonePaymentService))))
      }
    }

    "fail validation" when {
      "given missing data represented by an empty Map" in {

        BusinessActivities.formReads.validate(Map.empty) must
          be(Invalid(Seq((Path \ "businessActivities") -> Seq(ValidationError("error.required.bm.register.service")))))
      }

      "given invalid data" in {

        val model = Map("businessActivities[]" -> Seq("01", "99", "03"))

        BusinessActivities.formReads.validate(model) must
          be(Invalid(Seq((Path \ "businessActivities" \ 1 \ "businessActivities") -> Seq(ValidationError("error.invalid")))))
      }
    }

    "write correct data for businessActivities value" in {

      BusinessActivities.formWrites.writes(BusinessActivities(Set(EstateAgentBusinessService, BillPaymentServices, MoneyServiceBusiness))) must
        be(Map("businessActivities[]" -> Seq("03", "02", "05")))
    }

    "write correct data for businessActivities value on checkbox selected" in {

      BusinessActivities.formWrites.writes(BusinessActivities(Set(AccountancyServices))) must
        be(Map("businessActivities[]" -> Seq("01")))
    }

    "write correct data for businessActivities value when 3 checkbox selected" in {

      BusinessActivities.formWrites.writes(BusinessActivities(Set(TelephonePaymentService, TrustAndCompanyServices, HighValueDealing))) must
        be(Map("businessActivities[]" -> Seq("07", "06", "04")))
    }

    "get the value for each activity type" in {
      val ba = BusinessActivities(Set(EstateAgentBusinessService, AccountancyServices, HighValueDealing,
        MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
      ba.getValue(EstateAgentBusinessService) must be("03")
      ba.getValue(AccountancyServices) must be("01")
      ba.getValue(HighValueDealing) must be("04")
      ba.getValue(MoneyServiceBusiness) must be("05")
      ba.getValue(TrustAndCompanyServices) must be("06")
      ba.getValue(TelephonePaymentService) must be("07")

    }

    "get the message for each activity type" in {
      AccountancyServices.getMessage must be(Messages("businessmatching.registerservices.servicename.lbl.01"))
      BillPaymentServices.getMessage must be(Messages("businessmatching.registerservices.servicename.lbl.02"))
      EstateAgentBusinessService.getMessage must be(Messages("businessmatching.registerservices.servicename.lbl.03"))
      HighValueDealing.getMessage must be(Messages("businessmatching.registerservices.servicename.lbl.04"))
      MoneyServiceBusiness.getMessage must be(Messages("businessmatching.registerservices.servicename.lbl.05"))
      TrustAndCompanyServices.getMessage must be(Messages("businessmatching.registerservices.servicename.lbl.06"))
      TelephonePaymentService.getMessage must be(Messages("businessmatching.registerservices.servicename.lbl.07"))

    }

    "JSON validation" must {

      "successfully validate given an enum value" in {
        val json = Json.obj("businessActivities" -> Seq("05", "06", "07"))

        Json.fromJson[BusinessActivities](json) must
          be(JsSuccess(BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService)), JsPath \ "businessActivities"))

        Json.fromJson[BusinessActivities](Json.obj("businessActivities" -> Seq("01", "02", "03"))) must
          be(JsSuccess(BusinessActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService)), JsPath \ "businessActivities"))

        Json.fromJson[BusinessActivities](Json.obj("businessActivities" -> Seq("04"))) must
          be(JsSuccess(BusinessActivities(Set(HighValueDealing)), JsPath \ "businessActivities"))

      }

      "fail when on invalid data" in {
        Json.fromJson[BusinessActivities](Json.obj("businessActivity" -> "01")) must
          be(JsError((JsPath \ "businessActivities") -> play.api.data.validation.ValidationError("error.path.missing")))
      }
    }

    "validate json write" in {
      Json.toJson(BusinessActivities(Set(HighValueDealing, EstateAgentBusinessService))) must
        be(Json.obj("businessActivities" -> Seq("04", "03")))
    }

    "successfully validate json write" in {
      val json = Json.obj("businessActivities" -> Seq("02", "07", "01"))
      Json.toJson(BusinessActivities(Set(BillPaymentServices, TelephonePaymentService, AccountancyServices))) must be(json)

    }

    "throw error for invalid data" in {
      Json.fromJson[BusinessActivities](Json.obj("businessActivities" -> Seq(JsString("20")))) must
        be(JsError((JsPath \ "businessActivities") (0) \ "businessActivities", play.api.data.validation.ValidationError("error.invalid")))
    }
  }
}