/*
 * Copyright 2021 HM Revenue & Customs
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

import jto.validation.{Write, From, Rule}
import jto.validation.forms._
import play.api.libs.json.Json

case class IdentifyLinkedTransactions (linkedTxn: Boolean)

object IdentifyLinkedTransactions {

  import utils.MappingUtils.Implicits._

  implicit val format =  Json.format[IdentifyLinkedTransactions]

  implicit val formRule: Rule[UrlFormEncoded, IdentifyLinkedTransactions] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "linkedTxn").read[Boolean].withMessage("error.required.msb.linked.txn") map IdentifyLinkedTransactions.apply
  }

  implicit val formWrites: Write[IdentifyLinkedTransactions, UrlFormEncoded] = Write {x =>
    "linkedTxn" -> x.linkedTxn.toString
  }
}
