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

package models.asp

import models.DateOfChange
import models.asp.Service._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._

import java.time.LocalDate

class ServicesOfBusinessSpec extends PlaySpec with MockitoSugar {

  val businessServices: Set[Service] = Set(Accountancy, PayrollServices, BookKeeping, Auditing, FinancialOrTaxAdvice)

  "JSON validation" must {

    "successfully validate and read services and date of change values" in {

      val json = Json.obj("services" -> Seq("01", "02", "03", "04", "05"), "dateOfChange" -> "2016-02-24")

      Json.fromJson[ServicesOfBusiness](json) must
        be(JsSuccess(ServicesOfBusiness(businessServices, Some(DateOfChange(LocalDate.of(2016, 2, 24)))), JsPath))
    }

    "successfully validate selected services value" in {

      val json = Json.obj("services" -> Seq("01", "02", "03", "04", "05"))

      Json.fromJson[ServicesOfBusiness](json) must
        be(JsSuccess(ServicesOfBusiness(businessServices, None)))
    }

    "fail when on invalid data" in {

      Json.fromJson[ServicesOfBusiness](Json.obj("services" -> Seq("40"))) must
        be(JsError(((JsPath \ "services")(0) \ "services") -> play.api.libs.json.JsonValidationError("error.invalid")))
    }

    "successfully validate json write" in {

      val json = Json.obj("services" -> Seq("04", "05", "03", "02", "01"))
      Json.toJson(ServicesOfBusiness(businessServices)) must be(json)
    }
  }
}
