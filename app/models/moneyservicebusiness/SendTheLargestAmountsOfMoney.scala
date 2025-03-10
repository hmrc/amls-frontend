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

import models.Country
import play.api.libs.json.{Json, Reads, Writes}

case class SendTheLargestAmountsOfMoney(countries: Seq[Country])

private sealed trait SendTheLargestAmountsOfMoney0 {

  val jsonR: Reads[SendTheLargestAmountsOfMoney] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    ((__ \ "country_1").read[Country] and
      (__ \ "country_2").readNullable[Country] and
      (__ \ "country_3").readNullable[Country]).tupled map { countries =>
      SendTheLargestAmountsOfMoney(countries._1 +: Seq(countries._2, countries._3).flatten)
    }
  }

  val jsonW = Writes[SendTheLargestAmountsOfMoney] { lom =>
    lom.countries match {
      case Seq(a, b, c) => Json.obj("country_1" -> a, "country_2" -> b, "country_3" -> c)
      case Seq(a, b)    => Json.obj("country_1" -> a, "country_2" -> b)
      case Seq(a)       => Json.obj("country_1" -> a)
      case _            => Json.obj()
    }
  }
}

object SendTheLargestAmountsOfMoney {

  private object Cache extends SendTheLargestAmountsOfMoney0

  implicit val jsonR: Reads[SendTheLargestAmountsOfMoney]  = Cache.jsonR
  implicit val jsonW: Writes[SendTheLargestAmountsOfMoney] = Cache.jsonW
}
