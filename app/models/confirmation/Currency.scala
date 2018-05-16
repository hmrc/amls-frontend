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

package models.confirmation

case class Currency(value: BigDecimal) {

  override def toString: String =
    f"£$value%1.2f"

  def map(fn: BigDecimal => BigDecimal): Currency = fn(value)
}

object Currency {

  implicit def fromBD(value: BigDecimal): Currency = Currency(value)

  implicit def fromBDO(value: Option[BigDecimal]): Option[Currency] = value.map(Currency(_))

  implicit def fromInt(value: Int): Currency = Currency(value)

  implicit def currencyToDouble(c: Currency): Double = c.value.toDouble

  implicit def currencyToFloat(c: Currency): Float = c.value.toFloat

}
