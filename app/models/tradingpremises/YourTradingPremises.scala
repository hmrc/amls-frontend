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

package models.tradingpremises

import models.DateOfChange
import org.joda.time.LocalDate
import play.api.libs.json.{Reads, Writes}

case class YourTradingPremises(
                                tradingName: String,
                                tradingPremisesAddress: Address,
                                isResidential: Option[Boolean] = None,
                                startDate: Option[LocalDate] = None,
                                tradingNameChangeDate: Option[DateOfChange] = None
                              )

object YourTradingPremises {
  
  import play.api.libs.json.JodaReads._
  import play.api.libs.json.JodaWrites._

  implicit val reads: Reads[YourTradingPremises] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "tradingName").read[String] and
        __.read[Address] and
        (__ \ "isResidential").readNullable[Boolean] and
        (__ \ "startDate").readNullable[LocalDate] and
        (__ \ "tradingNameChangeDate").readNullable[DateOfChange]
      ) (YourTradingPremises.apply _)
  }

  implicit val writes: Writes[YourTradingPremises] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "tradingName").write[String] and
        __.write[Address] and
        (__ \ "isResidential").writeNullable[Boolean] and
        (__ \ "startDate").writeNullable[LocalDate] and
        (__ \ "tradingNameChangeDate").writeNullable[DateOfChange]
      ) (unlift(YourTradingPremises.unapply))
  }

  implicit def convert(data: YourTradingPremises): Option[TradingPremises] = {
    Some(TradingPremises(yourTradingPremises = Some(data)))
  }
}
