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

import models.Country
import play.api.libs.json.{Reads, Writes, __}

case class MostTransactions(countries: Seq[Country])

private sealed trait MostTransactions0 {

  val jsonReads: Reads[MostTransactions] =
    (__ \ "mostTransactionsCountries").readNullable[Seq[Country]].map {
      case Some(countries) if countries.isEmpty => MostTransactions(Seq())
      case Some(countries)                      => MostTransactions apply countries
      case None                                 => MostTransactions(Seq())
    }

  val jsonW: Writes[MostTransactions] = {
    import play.api.libs.functional.syntax.unlift
    (__ \ "mostTransactionsCountries").write[Seq[Country]] contramap unlift(MostTransactions.unapply)
  }
}

object MostTransactions {

  private object Cache extends MostTransactions0

  implicit val jsonR: Reads[MostTransactions]  = Cache.jsonReads
  implicit val jsonW: Writes[MostTransactions] = Cache.jsonW

  implicit def convert(model: MostTransactions): models.moneyservicebusiness.MostTransactions =
    models.moneyservicebusiness.MostTransactions(model.countries)
}
