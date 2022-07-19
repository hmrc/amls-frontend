/*
 * Copyright 2022 HM Revenue & Customs
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

import java.sql.Timestamp

import org.joda.time.{LocalDate, LocalDateTime}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsObject, JsResult, JsSuccess, Json}

class ReadStatusResponseSpec extends PlaySpec with MockitoSugar {

val mandatoryJson: JsObject = Json.obj (
    "processingDate" -> "2019-12-30T00:00:00Z",
    "formBundleStatus" -> "bundle",
    "statusReason" -> "status",
    "deRegistrationDate" -> "2017-01-01",
    "currentRegYearStartDate" -> "2018-02-02",
    "currentRegYearEndDate" -> "2019-03-03",
    "renewalConFlag" -> true
  )

  val fullJson: JsObject = Json.obj (
    "processingDate" -> "2019-12-30T00:00:00Z",
    "formBundleStatus" -> "bundle",
    "statusReason" -> "status",
    "deRegistrationDate" -> "2017-01-01",
    "currentRegYearStartDate" -> "2018-02-02",
    "currentRegYearEndDate" -> "2019-03-03",
    "renewalConFlag" -> true,
    "renewalSubmissionFlag" -> true,
    "currentAMLSOutstandingBalance" -> "balance",
    "businessContactNumber" -> "number",
    "safeId" -> "idNumber"
  )

  val mandatoryResponse = ReadStatusResponse (
    processingDate = new LocalDateTime(Timestamp.valueOf("2019-12-30 00:00:00.000")),
    formBundleStatus = "bundle",
    statusReason = Some("status"),
    deRegistrationDate = Some(new LocalDate("2017-01-01")),
    currentRegYearStartDate = Some(new LocalDate("2018-02-02")),
    currentRegYearEndDate = Some(new LocalDate("2019-03-03")),
    renewalConFlag = true
  )

  val fullResponse = ReadStatusResponse(
    processingDate = new LocalDateTime(Timestamp.valueOf("2019-12-30 00:00:00.000")),
    formBundleStatus = "bundle",
    statusReason = Some("status"),
    deRegistrationDate = Some(new LocalDate("2017-01-01")),
    currentRegYearStartDate = Some(new LocalDate("2018-02-02")),
    currentRegYearEndDate = Some(new LocalDate("2019-03-03")),
    renewalConFlag = true,
    renewalSubmissionFlag = Some(true),
    currentAMLSOutstandingBalance = Some("balance"),
    businessContactNumber = Some("number"),
    safeId = Some("idNumber")
  )



  "ReadStatusResponse" must {
    "serialise to json with mandatory fields" in {
      Json toJson mandatoryResponse mustEqual mandatoryJson
    }

    "serialise to json with all fields" in {
      Json toJson fullResponse mustEqual fullJson
    }

    "create model from json with all fields" in {
      val actual: JsResult[ReadStatusResponse] = Json.fromJson[ReadStatusResponse](fullJson)

      actual mustEqual JsSuccess(fullResponse)
    }

    "create model from json with only mandatory fields" in {
      val actual: JsResult[ReadStatusResponse] = Json.fromJson[ReadStatusResponse](mandatoryJson)

      actual mustEqual JsSuccess(mandatoryResponse)
    }
  }
}
