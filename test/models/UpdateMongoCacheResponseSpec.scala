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

package models

import models.businesscustomer.{Address, ReviewDetails}
import models.businessdetails._
import models.businessmatching.BusinessActivity.{MoneyServiceBusiness, TelephonePaymentService, TrustAndCompanyServices}
import models.businessmatching.BusinessMatchingMsbService.{ChequeCashingNotScrapMetal, ChequeCashingScrapMetal, CurrencyExchange, TransmittingMoney}
import models.businessmatching.BusinessType.SoleProprietor
import models.businessmatching._
import play.api.libs.json.{JsNull, JsSuccess, Json}
import utils.AmlsSpec

import java.time.LocalDate

class UpdateMongoCacheResponseSpec extends AmlsSpec {

  val response = UpdateMongoCacheResponse(
    None,
    view = None,
    businessMatching = Some(
      BusinessMatching(
        Some(
          ReviewDetails(
            "BusinessName",
            Some(SoleProprietor),
            Address(
              "line1",
              Some("line2"),
              Some("line3"),
              Some("line4"),
              Some("AA11 1AA"),
              Country("United Kingdom", "GB")
            ),
            "XE0000000000000",
            None
          )
        ),
        Some(
          BusinessActivities(
            Set(MoneyServiceBusiness, TrustAndCompanyServices, TelephonePaymentService),
            None,
            None,
            None
          )
        ),
        Some(
          BusinessMatchingMsbServices(
            Set(TransmittingMoney, CurrencyExchange, ChequeCashingNotScrapMetal, ChequeCashingScrapMetal)
          )
        ),
        Some(TypeOfBusiness("test")),
        Some(CompanyRegistrationNumber("12345678")),
        Some(BusinessAppliedForPSRNumberYes("123456")),
        false,
        true,
        true
      )
    ),
    estateAgencyBusiness = None,
    tradingPremises = None,
    businessDetails = Some(
      BusinessDetails(
        Some(PreviouslyRegisteredYes(Some("12345678"))),
        Some(ActivityStartDate(LocalDate.of(1990, 2, 24))),
        Some(VATRegisteredYes("123456789")),
        Some(CorporationTaxRegisteredYes("1234567890")),
        Some(ContactingYou(Some("1234567890"), Some("test@test.com"))),
        None,
        Some(RegisteredOfficeUK("38B", None, None, None, "AA1 1AA", None)),
        Some(true),
        None,
        Some(
          CorrespondenceAddress(
            Some(
              CorrespondenceAddressUk(
                "Name",
                "Business Name",
                "address 1",
                Some("address 2"),
                Some("address 3"),
                Some("address 4"),
                "AA11 1AA"
              )
            ),
            None
          )
        ),
        false,
        true
      )
    ),
    bankDetails = None,
    addPerson = None,
    businessActivities = None,
    responsiblePeople = None,
    tcsp = None,
    asp = None,
    msb = None,
    hvd = None,
    amp = None,
    supervision = None,
    Subscription = None,
    amendVariationResponse = None
  )

  val jsonBusinessMatching = Json.obj(
    "businessActivities"        -> Seq("05", "06", "07"),
    "msbServices"               -> Seq("01", "02", "03", "04"),
    "businessName"              -> "BusinessName",
    "businessType"              -> "Sole Trader",
    "businessAddress"           -> Json.obj(
      "line_1"   -> "line1",
      "line_2"   -> "line2",
      "line_3"   -> "line3",
      "line_4"   -> "line4",
      "postcode" -> "AA11 1AA",
      "country"  -> "GB"
    ),
    "safeId"                    -> "XE0000000000000",
    "typeOfBusiness"            -> "test",
    "companyRegistrationNumber" -> "12345678",
    "appliedFor"                -> true,
    "regNumber"                 -> "123456",
    "hasChanged"                -> false,
    "hasAccepted"               -> true,
    "preAppComplete"            -> true
  )

  val jsonBusinessDetails = Json.obj(
    "previouslyRegistered"     -> Json.obj("previouslyRegistered" -> true, "prevMLRRegNo" -> "12345678"),
    "activityStartDate"        -> Json.obj("startDate" -> "1990-02-24"),
    "vatRegistered"            -> Json.obj("registeredForVAT" -> true, "vrnNumber" -> "123456789"),
    "corporationTaxRegistered" -> Json
      .obj("registeredForCorporationTax" -> true, "corporationTaxReference" -> "1234567890"),
    "contactingYou"            -> Json.obj("phoneNumber" -> "1234567890", "email" -> "test@test.com"),
    "registeredOffice"         -> Json.obj(
      "addressLine1" -> "38B",
      "addressLine2" -> JsNull,
      "addressLine3" -> JsNull,
      "addressLine4" -> JsNull,
      "postCode"     -> "AA1 1AA",
      "dateOfChange" -> JsNull
    ),
    "altCorrespondenceAddress" -> true,
    "correspondenceAddress"    -> Json.obj(
      "yourName"                   -> "Name",
      "businessName"               -> "Business Name",
      "correspondenceAddressLine1" -> "address 1",
      "correspondenceAddressLine2" -> "address 2",
      "correspondenceAddressLine3" -> "address 3",
      "correspondenceAddressLine4" -> "address 4",
      "correspondencePostCode"     -> "AA11 1AA"
    ),
    "hasChanged"               -> false,
    "hasAccepted"              -> true
  )

  val oldJson = Json.obj(
    "etmpFormBundleNumber" -> "1111111111",
    "businessMatching"     -> jsonBusinessMatching,
    "aboutTheBusiness"     -> jsonBusinessDetails
  )

  "UpdateMongoCacheResponse" must {
    "read response with legacy fields" in {
      UpdateMongoCacheResponse.reads.reads(oldJson) must be(JsSuccess(response))
    }
  }
}
