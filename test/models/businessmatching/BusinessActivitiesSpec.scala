/*
 * Copyright 2019 HM Revenue & Customs
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
import models.DateOfChange
import org.joda.time.LocalDate
import play.api.i18n.Messages
import play.api.libs.json._
import utils.AmlsSpec


class BusinessActivitiesSpec extends AmlsSpec with MockitoSugar {
  import jto.validation.forms.Rules._

  "The BusinessActivities model" must {
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

    "write correct data for businessActivities value" when {

      "additionalActivities are not present" when {

        "single activities selected" in {

          BusinessActivities.formWrites.writes(BusinessActivities(Set(AccountancyServices))) must
            be(Map("businessActivities[]" -> Seq("01")))
        }

        "multiple activities selected" in {
          BusinessActivities.formWrites.writes(BusinessActivities(Set(TelephonePaymentService, TrustAndCompanyServices, HighValueDealing))) must
            be(Map("businessActivities[]" -> Seq("07", "06", "04")))
        }

      }
      "additionalActivities are present" when {

        "single activities selected" in {

          BusinessActivities.formWrites.writes(BusinessActivities(Set(AccountancyServices), Some(Set(HighValueDealing)))) must
            be(Map("businessActivities[]" -> Seq("04")))
        }

        "multiple activities selected" in {
          BusinessActivities.formWrites.writes(BusinessActivities(
            Set(TelephonePaymentService, TrustAndCompanyServices, HighValueDealing),
            Some(Set(AccountancyServices, TelephonePaymentService))
          )) must be(Map("businessActivities[]" -> Seq("01", "07")))
        }

      }
    }

    "get the value for each activity type" in {
      val ba = BusinessActivities(Set(EstateAgentBusinessService, AccountancyServices, HighValueDealing,
        MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))
      BusinessActivities.getValue(EstateAgentBusinessService) must be("03")
      BusinessActivities.getValue(AccountancyServices) must be("01")
      BusinessActivities.getValue(HighValueDealing) must be("04")
      BusinessActivities.getValue(MoneyServiceBusiness) must be("05")
      BusinessActivities.getValue(TrustAndCompanyServices) must be("06")
      BusinessActivities.getValue(TelephonePaymentService) must be("07")

    }

    "get the message for each activity type" in {
      AccountancyServices.getMessage(false) must be(Messages("businessmatching.registerservices.servicename.lbl.01"))
      BillPaymentServices.getMessage(false) must be(Messages("businessmatching.registerservices.servicename.lbl.03"))
      EstateAgentBusinessService.getMessage(false) must be(Messages("businessmatching.registerservices.servicename.lbl.04"))
      HighValueDealing.getMessage(false) must be(Messages("businessmatching.registerservices.servicename.lbl.05"))
      MoneyServiceBusiness.getMessage(false) must be(Messages("businessmatching.registerservices.servicename.lbl.06"))
      TrustAndCompanyServices.getMessage(false) must be(Messages("businessmatching.registerservices.servicename.lbl.07"))
      TelephonePaymentService.getMessage(false) must be(Messages("businessmatching.registerservices.servicename.lbl.08"))
    }

    "get the phrased message for each activity type" in {
      AccountancyServices.getMessage(true) must be(Messages("businessmatching.registerservices.servicename.lbl.01.phrased"))
      BillPaymentServices.getMessage(true) must be(Messages("businessmatching.registerservices.servicename.lbl.03.phrased"))
      EstateAgentBusinessService.getMessage(true) must be(Messages("businessmatching.registerservices.servicename.lbl.04.phrased"))
      HighValueDealing.getMessage(true) must be(Messages("businessmatching.registerservices.servicename.lbl.05.phrased"))
      MoneyServiceBusiness.getMessage(true) must be(Messages("businessmatching.registerservices.servicename.lbl.06.phrased"))
      TrustAndCompanyServices.getMessage(true) must be(Messages("businessmatching.registerservices.servicename.lbl.07.phrased"))
      TelephonePaymentService.getMessage(true) must be(Messages("businessmatching.registerservices.servicename.lbl.08.phrased"))
    }

    "JSON validation" when {

      "additionalActivities are not present" must {

        "successfully validate given an enum value" in {
          val json = Json.obj("businessActivities" -> Seq("05", "06", "07"))

          Json.fromJson[BusinessActivities](json) must
            be(JsSuccess(BusinessActivities(Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService))))

          Json.fromJson[BusinessActivities](Json.obj("businessActivities" -> Seq("01", "02", "03"))) must
            be(JsSuccess(BusinessActivities(Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))))

          Json.fromJson[BusinessActivities](Json.obj("businessActivities" -> Seq("04"))) must
            be(JsSuccess(BusinessActivities(Set(HighValueDealing))))

        }

        "fail when on invalid data" in {
          Json.fromJson[BusinessActivities](Json.obj("businessActivity" -> "01")) must
            be(JsError((JsPath \ "businessActivities") -> play.api.data.validation.ValidationError("error.path.missing")))
        }
      }

      "additionalActivities are present" must {

        "successfully validate given an enum value" in {

          Json.fromJson[BusinessActivities](Json.obj("businessActivities" -> Seq("05", "06", "07"), "additionalActivities" -> Seq("01", "02"))) must
            be(JsSuccess(
              BusinessActivities(
                Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService),
                Some(Set(AccountancyServices, BillPaymentServices))
              )
            ))

          Json.fromJson[BusinessActivities](Json.obj("businessActivities" -> Seq("01", "02", "03"), "additionalActivities" -> Seq("04", "05", "06"))) must
            be(JsSuccess(
              BusinessActivities(
                Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService),
                Some(Set(HighValueDealing, MoneyServiceBusiness, TrustAndCompanyServices))
              )
            ))

          Json.fromJson[BusinessActivities](Json.obj("businessActivities" -> Seq("04"), "additionalActivities" -> Seq("07"))) must
            be(JsSuccess(BusinessActivities(Set(HighValueDealing), Some(Set(TelephonePaymentService)))))

        }

        "fail given invalid data" in {
          Json.fromJson[BusinessActivities](Json.obj("businessActivities" -> Seq("01"),  "additionalActivities" -> Seq("11"))) must
            be(JsError((JsPath \ "additionalActivities") -> play.api.data.validation.ValidationError("error.invalid")))
        }
      }

      "removeActivities are present" must {

        "successfully validate given an enum value" in {

          Json.fromJson[BusinessActivities](Json.obj("businessActivities" -> Seq("05", "06", "07"), "removeActivities" -> Seq("01", "02"))) must
            be(JsSuccess(
              BusinessActivities(
                Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService),
                None,
                Some(Set(AccountancyServices, BillPaymentServices))
              )
            ))

          Json.fromJson[BusinessActivities](Json.obj("businessActivities" -> Seq("01", "02", "03"), "removeActivities" -> Seq("04", "05", "06"))) must
            be(JsSuccess(
              BusinessActivities(
                Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService),
                None,
                Some(Set(HighValueDealing, MoneyServiceBusiness, TrustAndCompanyServices))
              )
            ))

          Json.fromJson[BusinessActivities](Json.obj("businessActivities" -> Seq("04"), "removeActivities" -> Seq("07"))) must
            be(JsSuccess(BusinessActivities(Set(HighValueDealing), None, Some(Set(TelephonePaymentService)))))

        }

        "fail given invalid data" in {
          Json.fromJson[BusinessActivities](Json.obj("businessActivities" -> Seq("01"),  "removeActivities" -> Seq("11"))) must
            be(JsError((JsPath \ "removeActivities") -> play.api.data.validation.ValidationError("error.invalid")))
        }
      }

      "dateOfChange is present" must {

        "successfully valida given a date" in {

          val json = Json.obj(
            "businessActivities" -> Seq("05", "06", "07"),
            "dateOfChange" -> "1990-02-24"
          )

          Json.fromJson[BusinessActivities](json) must
            be(JsSuccess(BusinessActivities(
              Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService),
              None,
              None,
              Some(DateOfChange(new LocalDate(1990, 2,24)))
            )))

        }

      }
    }

    "validate json write" when {

      "additionalActivities are not present" in {
        Json.toJson(BusinessActivities(Set(HighValueDealing, EstateAgentBusinessService))) must
          be(Json.obj("businessActivities" -> Seq("04", "03")))
      }

      "additionalActivities are present" in {
        Json.toJson(BusinessActivities(Set(HighValueDealing, EstateAgentBusinessService), Some(Set(AccountancyServices, BillPaymentServices)))) must
          be(Json.obj("businessActivities" -> Seq("04", "03"), "additionalActivities" -> Seq("01", "02")))
      }

      "removeActivities are present" in {
        Json.toJson(BusinessActivities(Set(HighValueDealing, EstateAgentBusinessService), None, Some(Set(AccountancyServices, BillPaymentServices)))) must
          be(Json.obj("businessActivities" -> Seq("04", "03"), "removeActivities" -> Seq("01", "02")))
      }

      "dateOfChange is present" in {
        Json.toJson(BusinessActivities(
          Set(HighValueDealing, EstateAgentBusinessService),
          None,
          None,
          Some(DateOfChange(new LocalDate(1990, 2,24)))
        )) must be(Json.obj(
          "businessActivities" -> Seq("04", "03"),
          "dateOfChange" -> "1990-02-24"
        ))
      }

    }

    "throw error for invalid data" in {
      Json.fromJson[BusinessActivities](Json.obj("businessActivities" -> Seq(JsString("20")))) must
        be(JsError(JsPath \ "businessActivities", play.api.data.validation.ValidationError("error.invalid")))
    }
  }

  "The hasBusinessOrAdditionalActivity method" must {
    "return true" when {
      "only businessActivities contains the activity" in {
        val model = BusinessActivities(Set(AccountancyServices, MoneyServiceBusiness))

        model.hasBusinessOrAdditionalActivity(AccountancyServices) mustBe true
      }

      "only additionalActivities contains the activity" in {
        val model = BusinessActivities(Set(AccountancyServices), Some(Set(HighValueDealing)))

        model.hasBusinessOrAdditionalActivity(HighValueDealing) mustBe true
      }
    }

    "return false" when {
      "neither businessActivities or additionalActivities contains the activity" in {
        val model = BusinessActivities(Set(AccountancyServices), Some(Set(MoneyServiceBusiness)))

        model.hasBusinessOrAdditionalActivity(HighValueDealing) mustBe false
      }
    }
  }
}