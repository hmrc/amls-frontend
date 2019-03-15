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

import jto.validation._
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json._

case class SupplyForeignCurrencies(bankMoneySource: Option[BankMoneySource],
                           wholesalerMoneySource: Option[WholesalerMoneySource],
                           customerMoneySource: Option[CustomerMoneySource])


object SupplyForeignCurrencies {

  implicit def formRule: Rule[UrlFormEncoded, SupplyForeignCurrencies] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._

    ((__ \ "bankMoneySource").read[Option[BankMoneySource]] ~
      (__ \ "wholesalerMoneySource").read[Option[WholesalerMoneySource]] ~
      (__ \ "customerMoneySource").read[Option[CustomerMoneySource]]).apply(SupplyForeignCurrencies.apply _)
  }

  implicit val formWrite: Write[SupplyForeignCurrencies, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._

      ((__ \ "bankMoneySource").write[Option[BankMoneySource]] ~
        (__ \ "wholesalerMoneySource").write[Option[WholesalerMoneySource]] ~
        (__ \ "customerMoneySource").write[Option[CustomerMoneySource]]
      ).apply(wc => (wc.bankMoneySource, wc.wholesalerMoneySource, wc.customerMoneySource))
  }

  implicit val jsonReads: Reads[SupplyForeignCurrencies] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._

      (__.read[Option[BankMoneySource]] and
        __.read[Option[WholesalerMoneySource]] and
        (__ \ "customerMoneySource").readNullable[CustomerMoneySource]
      )((bms, wms, cms) => SupplyForeignCurrencies(bms, wms, cms))
  }

  implicit val jsonWrites: Writes[SupplyForeignCurrencies] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    (__.write[Option[BankMoneySource]] and
      __.write[Option[WholesalerMoneySource]] and
        __.write[Option[CustomerMoneySource]])(x => (x.bankMoneySource, x.wholesalerMoneySource, x.customerMoneySource))
  }
}
