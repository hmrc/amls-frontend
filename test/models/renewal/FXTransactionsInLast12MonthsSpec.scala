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

package models.renewal

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, JsSuccess}

class FXTransactionsInLast12MonthsSpec extends PlaySpec {

  "FXTransactionIn" should {

    "Json Validation" must {

      "Successfully read/write Json data" in {
        FXTransactionsInLast12Months.format.reads(
          FXTransactionsInLast12Months.format.writes(FXTransactionsInLast12Months("12345678963"))
        ) must be(JsSuccess(FXTransactionsInLast12Months("12345678963"), JsPath))
      }
    }
  }
}
