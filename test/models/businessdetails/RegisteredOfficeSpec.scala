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

package models.businessdetails

import models.businesscustomer.Address
import models.{Country, DateOfChange}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsNull, JsPath, JsSuccess, Json}

import java.time.LocalDate

class RegisteredOfficeSpec extends PlaySpec with MockitoSugar {

  "RegisteredOffice" must {

    "validate toLines for UK address" in {
      RegisteredOfficeUK("38B", Some("some street"), None, None, "AA1 1AA").toLines must be(
        Seq("38B", "some street", "AA1 1AA")
      )

    }

    "validate toLines for Non UK address" in {
      RegisteredOfficeNonUK("38B", Some("some street"), None, None, Country("United Kingdom", "GB")).toLines must be(
        Seq("38B", "some street", "United Kingdom")
      )

    }

    "json read the given non UK address" in {

      val data    = RegisteredOfficeUK("38B", Some("area"), Some("line 1"), None, "AA1 1AA")
      val jsonObj = Json.obj("postCode" -> "AA1 1AA")

      Json.fromJson[RegisteredOffice](jsonObj) must be
      JsSuccess(data, JsPath)
    }

    "write correct value to json with date of change" in {
      val data = RegisteredOfficeUK(
        "38B",
        Some("area"),
        Some("line 1"),
        None,
        "AA1 1AA",
        Some(DateOfChange(LocalDate.of(2017, 1, 1)))
      )

      Json.toJson(data.asInstanceOf[RegisteredOffice]) must
        be(
          Json.obj(
            "addressLine1" -> "38B",
            "addressLine2" -> "area",
            "addressLine3" -> "line 1",
            "addressLine4" -> JsNull,
            "postCode"     -> "AA1 1AA",
            "dateOfChange" -> "2017-01-01"
          )
        )
    }

    "write correct value to json without date of change" in {
      val data = RegisteredOfficeUK("38B", Some("area"), Some("line 1"), None, "AA1 1AA", None)

      Json.toJson(data.asInstanceOf[RegisteredOffice]) must
        be(
          Json.obj(
            "addressLine1" -> "38B",
            "addressLine2" -> "area",
            "addressLine3" -> "line 1",
            "addressLine4" -> JsNull,
            "postCode"     -> "AA1 1AA",
            "dateOfChange" -> JsNull
          )
        )
    }

    val uKRegisteredOffice = RegisteredOfficeUK(
      "Test Address 1",
      Some("Test Address 2"),
      Some("Test Address 3"),
      Some("Test Address 4"),
      "P05TC0DE"
    )

    val nonUKRegisteredOffice = RegisteredOfficeNonUK(
      "Test Address 1",
      Some("Test Address 2"),
      Some("Test Address 3"),
      Some("Test Address 4"),
      Country("Albania", "AL")
    )

    "Round trip a UK Address correctly through serialisation" in {
      RegisteredOffice.jsonReads.reads(
        RegisteredOffice.jsonWrites.writes(uKRegisteredOffice)
      ) must be(JsSuccess(uKRegisteredOffice))
    }

    "Round trip a Non UK Address correctly through serialisation" in {
      RegisteredOffice.jsonReads.reads(
        RegisteredOffice.jsonWrites.writes(nonUKRegisteredOffice)
      ) must be(JsSuccess(nonUKRegisteredOffice))
    }

    "convert Business Customer Address to RegisteredOfficeUK" in {
      val address =
        Address("addr1", Some("addr2"), Some("line3"), Some("line4"), Some("AA1 1AA"), Country("United Kingdom", "GB"))

      RegisteredOffice.convert(address) must be(
        RegisteredOfficeUK("addr1", Some("addr2"), Some("line3"), Some("line4"), "AA1 1AA")
      )
    }

    "convert Business Customer Address to RegisteredOfficeNonUK" in {
      val address = Address("addr1", Some("addr2"), Some("line3"), Some("line4"), None, Country("Albania", "AL"))

      RegisteredOffice.convert(address) must be(
        RegisteredOfficeNonUK("addr1", Some("addr2"), Some("line3"), Some("line4"), Country("Albania", "AL"))
      )
    }
  }
}
