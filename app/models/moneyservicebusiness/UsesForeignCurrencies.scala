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

package models.moneyservicebusiness

import play.api.libs.json._

sealed trait UsesForeignCurrencies {
  val value: Boolean
}

case object UsesForeignCurrenciesYes extends UsesForeignCurrencies {
  override val value: Boolean = true
}

case object UsesForeignCurrenciesNo extends UsesForeignCurrencies {
  override val value: Boolean = false
}

object UsesForeignCurrencies {

  def fromBoolean(boolean: Boolean): UsesForeignCurrencies =
    if (boolean) UsesForeignCurrenciesYes else UsesForeignCurrenciesNo

  implicit val jsonReads: Reads[UsesForeignCurrencies] =
    (__ \ "foreignCurrencies").read[Boolean] flatMap {
      case true  => Reads(_ => JsSuccess(UsesForeignCurrenciesYes))
      case false => Reads(_ => JsSuccess(UsesForeignCurrenciesNo))
    }

  implicit val jsonWrites: Writes[UsesForeignCurrencies] = Writes[UsesForeignCurrencies] {
    case UsesForeignCurrenciesYes => Json.obj("foreignCurrencies" -> true)
    case UsesForeignCurrenciesNo  => Json.obj("foreignCurrencies" -> false)
  }
}
