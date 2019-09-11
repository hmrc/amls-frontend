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

package models.amp

import config.ApplicationConfig
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap

case class Amp(
                hasChanged: Boolean = false,
                hasAccepted: Boolean = false
              ) {

  //TODO: Implement the methods based on AMP frontend once confirmed and ready to sync

  def BoughtOrSoldOverThreshold(): Amp  = ???
  def DateTransactionOverThreshold(): Amp   = ???
  def IdentifyLinkedTransactionsView(): Amp   = ???
  def PercentageExpectedTurnover(): Amp   = ???
  def TypeOfParticipant(): Amp   = ???

  def isComplete: Boolean = this match {
    case Amp(true, accepted) => accepted
    case _ => false
  }
}

object Amp {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  implicit val formatOption = Reads.optionWithNull[Amp]

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "asp"
    val notStarted = Section(messageKey, NotStarted, false, controllers.asp.routes.WhatYouNeedController.get())
    cache.getEntry[Amp](key).fold(notStarted) {
      model =>
        if (model.isComplete) {
          Section(messageKey, Completed, model.hasChanged, controllers.asp.routes.SummaryController.get())
        } else {
          Section(messageKey, Started, model.hasChanged, controllers.asp.routes.WhatYouNeedController.get())
        }
    }
  }

  val key = "amp"

  implicit val mongoKey = new MongoKey[Amp] {
    override def apply(): String = "amp"
  }

  implicit val jsonWrites = Json.writes[Amp]

  implicit val jsonReads: Reads[Amp] = {
      (__ \ "hasChanged").readNullable[Boolean].map(_.getOrElse(false)) and
      (__ \ "hasAccepted").readNullable[Boolean].map(_.getOrElse(false))
    }.apply(Amp.apply _)

  implicit def default(details: Option[Amp]): Amp =
    details.getOrElse(Amp())
}
