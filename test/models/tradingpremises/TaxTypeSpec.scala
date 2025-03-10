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

package models.tradingpremises

import play.api.libs.json.{JsError, JsSuccess, Json, JsonValidationError}
import utils.AmlsSpec

class TaxTypeSpec extends AmlsSpec {

  "jsonWritesTaxType" must {
    "convert TaxTypeSelfAssesment to JSON" in {
      TaxType.jsonWritesTaxType.writes(TaxTypeSelfAssesment) mustEqual Json.obj(("taxType", Json.toJson("01")))
    }
    "convert TaxTypeCorporationTax to JSON" in {
      TaxType.jsonWritesTaxType.writes(TaxTypeCorporationTax) mustEqual Json.obj(("taxType", Json.toJson("02")))
    }
  }

  "jsonReadsTaxType" must {
    "convert valid JSON to TaxTypeSelfAssesment" in {
      TaxType.jsonReadsTaxType.reads(Json.obj(("taxType", Json.toJson("01")))) mustEqual JsSuccess(TaxTypeSelfAssesment)
    }
    "convert valid JSON to TaxTypeCorporationTax" in {
      TaxType.jsonReadsTaxType.reads(Json.obj(("taxType", Json.toJson("02")))) mustEqual JsSuccess(
        TaxTypeCorporationTax
      )
    }
    "return error for invalid JSON" in {
      TaxType.jsonReadsTaxType.reads(Json.obj(("taxType", Json.toJson("03")))) mustEqual JsError(
        JsonValidationError("error.invalid")
      )
    }
  }
}
