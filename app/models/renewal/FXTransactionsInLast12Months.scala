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

package models.renewal

import jto.validation.forms.Rules._
import jto.validation.forms._
import jto.validation.{From, Rule, Write}
import models.FormTypes._
import play.api.libs.json.Json

case class FXTransactionsInLast12Months(fxTransaction: String)

object FXTransactionsInLast12Months {

  import utils.MappingUtils.Implicits._

  implicit val format = Json.format[FXTransactionsInLast12Months]

  private val txnAmountRegex = regexWithMsg("^[0-9]{1,11}$".r, "error.invalid.msb.transactions.in.12months")
  private val txnAmountType = notEmptyStrip andThen
    notEmpty.withMessage("error.required.renewal.fx.transactions.in.12months") andThen txnAmountRegex

  implicit val formRule: Rule[UrlFormEncoded, FXTransactionsInLast12Months] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "fxTransaction").read(txnAmountType) map FXTransactionsInLast12Months.apply
  }

  implicit val formWrites: Write[FXTransactionsInLast12Months, UrlFormEncoded] = Write { x =>
    Map("fxTransaction" -> Seq(x.fxTransaction))
  }

  implicit def convert(model: FXTransactionsInLast12Months): models.moneyservicebusiness.FXTransactionsInNext12Months = {
    models.moneyservicebusiness.FXTransactionsInNext12Months(model.fxTransaction)
  }
}
