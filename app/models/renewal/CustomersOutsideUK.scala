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

import models.{Country, businessactivities}
import play.api.libs.json.{Json, Reads, Writes}

case class CustomersOutsideUK(countries: Option[Seq[Country]])

sealed trait CustomersOutsideUK0 {

  val jsonR: Reads[CustomersOutsideUK] =
    implicitly

  val jsonW = Writes[CustomersOutsideUK] { customer =>
    val countries = customer.countries.fold[Seq[String]](Seq.empty)(customer => customer.map(country => country.code))
    Json.obj(
      "countries" -> countries
    )
  }
}

object CustomersOutsideUK {

  private object Cache extends CustomersOutsideUK0

  implicit val jsonR: Reads[CustomersOutsideUK]  = Cache.jsonR
  implicit val jsonW: Writes[CustomersOutsideUK] = Cache.jsonW

  implicit def convert(model: CustomersOutsideUK): businessactivities.CustomersOutsideUK =
    models.businessactivities.CustomersOutsideUK(model.countries)
}
