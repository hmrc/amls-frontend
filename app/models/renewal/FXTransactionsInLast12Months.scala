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

import play.api.libs.json.{Json, OFormat}

case class FXTransactionsInLast12Months(fxTransaction: String)

object FXTransactionsInLast12Months {

  implicit val format: OFormat[FXTransactionsInLast12Months] = Json.format[FXTransactionsInLast12Months]

  implicit def convert(model: FXTransactionsInLast12Months): models.moneyservicebusiness.FXTransactionsInNext12Months =
    models.moneyservicebusiness.FXTransactionsInNext12Months(model.fxTransaction)
}
