/*
 * Copyright 2023 HM Revenue & Customs
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

package models.moneyservicebusiness

import models.FormTypes._
import jto.validation.{Write, From, Rule}
import jto.validation.forms.Rules._
import jto.validation.forms._
import play.api.libs.json.Json

case class FXTransactionsInNext12Months (fxTransaction: String)

object FXTransactionsInNext12Months {

    import utils.MappingUtils.Implicits._

    implicit val format = Json.format[FXTransactionsInNext12Months]

    private val maxCharLength = 11
    private val txnAmountType = notEmptyStrip andThen
      notEmpty.withMessage("error.required.msb.fx.transactions.in.12months") andThen
      maxLength(maxCharLength).withMessage("error.invalid.msb.fx.transactions.in.12months") andThen
      regexWithMsg("^[0-9]{1,11}".r, "error.invalid.msb.fx.transactions.in.12months.number")

    implicit val formRule: Rule[UrlFormEncoded, FXTransactionsInNext12Months] = From[UrlFormEncoded] { __ =>
        import jto.validation.forms.Rules._
        (__ \ "fxTransaction").read(txnAmountType) map FXTransactionsInNext12Months.apply
    }

    implicit val formWrites: Write[FXTransactionsInNext12Months, UrlFormEncoded] = Write {x =>
        Map("fxTransaction" -> Seq(x.fxTransaction))
    }
}
