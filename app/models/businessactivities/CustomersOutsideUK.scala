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

package models.businessactivities

import models.Country
import play.api.libs.json.{Json, Reads, Writes, __}

case class CustomersOutsideUK(countries: Option[Seq[Country]])

sealed trait CustomersOutsideUK0 {

  val jsonReads: Reads[CustomersOutsideUK] =
    (__ \ "countries").readNullable[Seq[Country]].map {
      case Some(countries) if countries.isEmpty => CustomersOutsideUK(None)
      case c @ Some(countries)                  => CustomersOutsideUK apply c
      case None                                 => CustomersOutsideUK(None)
    }

  val jsonW = Writes[CustomersOutsideUK] { x =>
    val countries = x.countries.fold[Seq[String]](Seq.empty)(x => x.map(m => m.code))
    countries.nonEmpty match {
      case true  =>
        Json.obj(
          "isOutside" -> true,
          "countries" -> countries
        )
      case false =>
        Json.obj(
          "isOutside" -> false
        )
    }
  }
}

object CustomersOutsideUK {

  private object Cache extends CustomersOutsideUK0

  implicit val jsonR: Reads[CustomersOutsideUK]  = Cache.jsonReads
  implicit val jsonW: Writes[CustomersOutsideUK] = Cache.jsonW
}
