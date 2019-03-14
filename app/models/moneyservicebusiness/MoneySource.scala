/*
 * Copyright 2019 HM Revenue & Customs
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

import cats.data.Validated.Valid
import jto.validation.GenericRules.{maxLength, minLength}
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule}
import models.FormTypes.{basicPunctuationPattern, notEmptyStrip}
import utils.MappingUtils.Implicits._

case class BankMoneySource(bankNames : String)

object BankMoneySource {

  private def nameType(fieldName: String) = {
    notEmptyStrip andThen
      minLength(1).withMessage(s"error.invalid.msb.wc.$fieldName") andThen
      maxLength(140).withMessage("error.invalid.maxlength.140") andThen
      basicPunctuationPattern()
  }

  implicit def formR: Rule[UrlFormEncoded, Option[BankMoneySource]] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._

    (__ \ "bankMoneySource").read[Option[String]] flatMap {
      case Some("Yes") => (__ \ "bankNames")
        .read(nameType("bankNames"))
        .map(names => Some(BankMoneySource(names)))
      case _ => Rule[UrlFormEncoded, Option[BankMoneySource]](_ => Valid(None))
    }
  }
}

case class WholesalerMoneySource(wholesalerNames : String)

object WholesalerMoneySource {

  private def nameType(fieldName: String) = {
    notEmptyStrip andThen
      minLength(1).withMessage(s"error.invalid.msb.wc.$fieldName") andThen
      maxLength(140).withMessage("error.invalid.maxlength.140") andThen
      basicPunctuationPattern()
  }

  implicit def formR: Rule[UrlFormEncoded, Option[WholesalerMoneySource]] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._

    (__ \ "wholesalerMoneySource").read[Option[String]] flatMap {
      case Some("Yes") => (__ \ "wholesalerNames")
        .read(nameType("wholesalerNames"))
        .map(names => Some(WholesalerMoneySource(names)))
      case _ => Rule[UrlFormEncoded, Option[WholesalerMoneySource]](_ => Valid(None))
    }
  }
}

case object CustomerMoneySource
