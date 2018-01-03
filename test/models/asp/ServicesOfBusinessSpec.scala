/*
 * Copyright 2018 HM Revenue & Customs
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

package models.asp

import models.DateOfChange
import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json._

class ServicesOfBusinessSpec extends PlaySpec with MockitoSugar {

  "Form validation" must {

    val businessServices: Set[Service] = Set(Accountancy, PayrollServices, BookKeeping, Auditing, FinancialOrTaxAdvice)

    import jto.validation.forms.Rules._

    "validate model with few check box selected" in {

      val model = Map(
        "services[]" -> Seq("01", "02", "03", "04", "05")
      )

      ServicesOfBusiness.formReads.validate(model) must
        be(Valid(ServicesOfBusiness(businessServices)))
    }

    "fail to validate on empty Map" in {

      ServicesOfBusiness.formReads.validate(Map.empty) must
        be(Invalid(Seq((Path \ "services") -> Seq(ValidationError("error.required.asp.business.services")))))

    }

    "fail to validate when given invalid data" in {
      val model = Map(
        "services[]" -> Seq("02", "99", "03")
      )

      ServicesOfBusiness.formReads.validate(model) must
        be(Invalid(Seq((Path \ "services" \ 1 \ "services", Seq(ValidationError("error.invalid"))))))
    }

    "write correct data for services value" in {

      ServicesOfBusiness.formWrites.writes(ServicesOfBusiness(Set(Accountancy, PayrollServices, BookKeeping))) must
        be(Map("services[]" -> Seq("01", "02", "03")))

      ServicesOfBusiness.formWrites.writes(ServicesOfBusiness(Set(Auditing, FinancialOrTaxAdvice))) must
        be(Map("services[]" -> Seq("04", "05")))

    }

    "JSON validation" must {

      "successfully validate and read services and date of change values" in {

        val json =  Json.obj("services" -> Seq("01","02","03","04","05"),
          "dateOfChange" -> "2016-02-24")

        Json.fromJson[ServicesOfBusiness](json) must
          be(JsSuccess(ServicesOfBusiness(businessServices, Some(DateOfChange(new LocalDate("2016-02-24")))), JsPath))
      }

      "successfully validate selected services value" in {

        val json =  Json.obj("services" -> Seq("01","02","03","04","05"))

        Json.fromJson[ServicesOfBusiness](json) must
          be(JsSuccess(ServicesOfBusiness(businessServices, None)))
      }

      "fail when on invalid data" in {

        Json.fromJson[ServicesOfBusiness](Json.obj("services" -> Seq("40"))) must
          be(JsError(((JsPath \ "services")(0) \ "services") -> play.api.data.validation.ValidationError("error.invalid")))
      }

      "successfully validate json write" in {

        val json = Json.obj("services" -> Set("01","02","03","04","05"))
        Json.toJson(ServicesOfBusiness(businessServices)) must be(json)
      }
    }

  }

}
