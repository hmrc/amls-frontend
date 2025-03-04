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

import org.scalatestplus.play.PlaySpec
import play.api.libs.json._
import utils.DependencyMocks
import generators.businessmatching.BusinessMatchingGenerator

class ViewResponseSpec extends PlaySpec with DependencyMocks with BusinessMatchingGenerator {

  "ViewResponse" must {
    val DefaultInvolvedInOtherDetails = "DEFAULT INVOLVED"
    val DefaultFranchiseName          = "DEFAULT FRANCHISE NAME"
    val DefaultSoftwareName           = "DEFAULT SOFTWARE"

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
        "addressLine2" -> "line2",
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

    val jsonBank = Seq(
      Json.obj(
        "bankAccountType"     -> Json.obj("bankAccountType" -> "01"),
        "accountName"         -> "bankName",
        "bankAccount"         -> Json.obj("isUK" -> true, "accountNumber" -> "111111", "sortCode" -> "00-00-00"),
        "hasChanged"          -> false,
        "refreshedFromServer" -> false,
        "hasAccepted"         -> true
      )
    )

    val jsonPerson = Json.obj(
      "firstName"          -> "FNAME",
      "lastName"           -> "LNAME",
      "roleWithinBusiness" -> "02"
    )

    val jsonActivities = Json.obj(
      "involvedInOther"              -> true,
      "details"                      -> DefaultInvolvedInOtherDetails,
      "expectedBusinessTurnover"     -> "01",
      "expectedAMLSTurnover"         -> "01",
      "businessFranchise"            -> true,
      "franchiseName"                -> DefaultFranchiseName,
      "isRecorded"                   -> true,
      "transactionTypes"             -> Json.obj(
        "types"    -> Seq("01", "03"),
        "software" -> DefaultSoftwareName
      ),
      "isOutside"                    -> true,
      "countries"                    -> Json.arr("GB"),
      "ncaRegistered"                -> true,
      "accountantForAMLSRegulations" -> true,
      "hasWrittenGuidance"           -> true,
      "hasPolicy"                    -> true,
      "riskassessments"              -> Seq("01"),
      "employeeCount"                -> "5",
      "employeeCountAMLSSupervision" -> "4",
      "accountantsName"              -> "Accountant's name",
      "accountantsTradingName"       -> "Accountant's trading name",
      "accountantsAddressLine1"      -> "address1",
      "accountantsAddressLine2"      -> "address2",
      "accountantsAddressLine3"      -> "address3",
      "accountantsAddressLine4"      -> "address4",
      "accountantsAddressPostCode"   -> "POSTCODE",
      "manageYourTaxAffairs"         -> false,
      "hasWrittenGuidance"           -> true,
      "hasChanged"                   -> false,
      "hasAccepted"                  -> true
    )

    val json = Json.obj(
      "etmpFormBundleNumber"      -> "1111111111",
      "businessMatchingSection"   -> jsonBusinessMatching,
      "businessDetailsSection"    -> jsonBusinessDetails,
      "bankDetailsSection"        -> jsonBank,
      "aboutYouSection"           -> jsonPerson,
      "businessActivitiesSection" -> jsonActivities
    )

    val oldJson = Json.obj(
      "etmpFormBundleNumber"      -> "1111111111",
      "businessMatchingSection"   -> jsonBusinessMatching,
      "aboutTheBusinessSection"   -> jsonBusinessDetails,
      "bankDetailsSection"        -> jsonBank,
      "aboutYouSection"           -> jsonPerson,
      "businessActivitiesSection" -> jsonActivities
    )

    "read from JSON correctly" in {
      val result = Json.fromJson[ViewResponse](json).get
      result mustBe a[ViewResponse]
    }

    "read from old JSON correctly" in {
      val result = Json.fromJson[ViewResponse](oldJson).get
      result mustBe a[ViewResponse]
    }
  }
}
