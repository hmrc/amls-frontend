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
import play.api.libs.json._
import typeclasses.MongoKey
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

case class AgentName(agentName: String,
                     dateOfChange: Option[DateOfChange] = None,
                     agentDateOfBirth: Option[LocalDate] = None
                    )

object AgentName {

  implicit val mongoKey = new MongoKey[AgentName] {
    override def apply(): String = "agent-name"
  }

  implicit val format = Json.format[AgentName]

  implicit def convert(data: AgentName): Option[TradingPremises] = {
    Some(TradingPremises(agentName = Some(data)))
  }
}
