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

package models.renewal

import jto.validation._
import jto.validation.forms.UrlFormEncoded
import jto.validation.forms.Rules.{minLength => _, _}
import play.api.libs.json._

sealed trait UsesForeignCurrencies

case object UsesForeignCurrenciesYes extends UsesForeignCurrencies

case object UsesForeignCurrenciesNo extends UsesForeignCurrencies

object UsesForeignCurrencies {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, UsesForeignCurrencies] =
    From[UrlFormEncoded] { __ =>
      (__ \ "usesForeignCurrencies").read[Boolean].withMessage("error.required.msb.wc.foreignCurrencies") map {
        case true => UsesForeignCurrenciesYes
        case false => UsesForeignCurrenciesNo
      }
    }

  implicit def formWrites = Write[UsesForeignCurrencies, UrlFormEncoded] {
    case UsesForeignCurrenciesYes => Map("usesForeignCurrencies" -> "true")
    case UsesForeignCurrenciesNo => Map("usesForeignCurrencies" -> "false")
  }

  implicit val jsonReads: Reads[UsesForeignCurrencies] = {
    (__ \ "foreignCurrencies").read[Boolean] flatMap {
      case true => Reads(_ => JsSuccess(UsesForeignCurrenciesYes))
      case false => Reads(_ => JsSuccess(UsesForeignCurrenciesNo))
    }
  }

  implicit val jsonWrites = Writes[UsesForeignCurrencies] {
    case UsesForeignCurrenciesYes => Json.obj("foreignCurrencies" -> true)
    case UsesForeignCurrenciesNo => Json.obj("foreignCurrencies" -> false)
  }
}