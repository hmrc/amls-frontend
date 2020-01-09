/*
 * Copyright 2020 HM Revenue & Customs
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

import cats.data.Validated.Valid
import jto.validation.{Invalid, Path, ValidationError}
import play.api.libs.json.{JsError, JsSuccess, Json}
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
      TaxType.jsonReadsTaxType.reads(Json.obj(("taxType", Json.toJson("02")))) mustEqual JsSuccess(TaxTypeCorporationTax)
    }
    "return error for invalid JSON" in {
      TaxType.jsonReadsTaxType.reads(Json.obj(("taxType", Json.toJson("03")))) mustEqual JsError(play.api.data.validation.ValidationError("error.invalid"))
    }
  }

  "taxTypeRule" must {

    "convert valid form to TaxTypeSelfAssesment" in {
      TaxType.taxTypeRule.validate(Map("taxType" -> Seq("01"))) mustEqual Valid(TaxTypeSelfAssesment)
    }

    "convert valid form to TaxTypeCorporationTax" in {
      TaxType.taxTypeRule.validate(Map("taxType" -> Seq("02"))) mustEqual Valid(TaxTypeCorporationTax)
    }

    "return error for invalid form" in {
      TaxType.taxTypeRule.validate(Map("taxType" -> Seq("03"))) mustEqual Invalid(Seq(Path \ "taxType" -> Seq(ValidationError("error.invalid"))))
    }
  }

  "formWritesTaxType" must {

      "convert TaxTypeSelfAssesment to form" in {
        TaxType.formWritesTaxType.writes(TaxTypeSelfAssesment) mustEqual Map("taxType" -> Seq("01"))
      }

      "convert TaxTypeCorporationTax to form" in {
        TaxType.formWritesTaxType.writes(TaxTypeCorporationTax) mustEqual Map("taxType" -> Seq("02"))
      }
  }
}
