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

import java.time.LocalDateTime
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import play.api.Mode.Mode
import play.api.{Configuration, Play}
import play.api.libs.json._
import play.api.mvc.Call
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.config.ServicesConfig

final case class Amp(_id: String,
                     data: JsObject = Json.obj(),
                     lastUpdated: LocalDateTime = LocalDateTime.now,
                     hasChanged: Boolean = false,
                     hasAccepted: Boolean = false) {

  /**
    * Provides a means of setting data that will update the hasChanged flag
    *
    * Set data via this method and NOT directly in the constructor
    */
  def data(p: JsObject): Amp =
    this.copy(data = p, hasChanged = hasChanged || this.data != p, hasAccepted = hasAccepted && this.data == p)

  val typeOfParticipant            = JsPath \ "typeOfParticipant"
  val typeOfParticipantDetail      = JsPath \ "typeOfParticipantDetail"
  val boughtOrSoldOverThreshold    = JsPath \ "boughtOrSoldOverThreshold"
  val identifyLinkedTransactions   = JsPath \ "identifyLinkedTransactions"
  val dateTransactionOverThreshold = JsPath \ "dateTransactionOverThreshold"
  val percentageExpectedTurnover   = JsPath \ "percentageExpectedTurnover"
  val otherTypeOfParticipant       = "somethingelse"

  private def get[A](path: JsPath)(implicit rds: Reads[A]): Option[A] =
    Reads.optionNoError(Reads.at(path)).reads(data).getOrElse(None)

  private def isDefinedAt(path: JsPath): Boolean = {
    get[JsValue](path).isDefined
  }

  private def valueAt(path: JsPath): String = {
    get[JsValue](path).getOrElse("").toString().toLowerCase()
  }

  private def isTypeOfParticipantComplete: Boolean = {
    isDefinedAt(typeOfParticipant) &&
      ((valueAt(typeOfParticipant).contains(otherTypeOfParticipant) &&
        isDefinedAt(typeOfParticipantDetail)) ||
        (!valueAt(typeOfParticipant).contains(otherTypeOfParticipant)))
  }

  private def isBoughtOrSoldOverThresholdComplete: Boolean = {
    isDefinedAt(boughtOrSoldOverThreshold) &&
      ((valueAt(boughtOrSoldOverThreshold) == "true" &&
        isDefinedAt(dateTransactionOverThreshold)) ||
        (valueAt(boughtOrSoldOverThreshold) == "false"))
  }

  def isComplete: Boolean = {
    isTypeOfParticipantComplete &&
    isBoughtOrSoldOverThresholdComplete &&
    isDefinedAt(identifyLinkedTransactions) &&
    isDefinedAt(percentageExpectedTurnover)
  }
}

object Amp extends ServicesConfig {

  val redirectCallType       = "GET"
  val key                    = "amp"
  lazy val ampWhatYouNeedUrl = s"${baseUrl("amls-art-market-participant-frontend")}/amls-art-market-participant-frontend/what-you-need"
  lazy val ampSummeryUrl     = s"${baseUrl("amls-art-market-participant-frontend")}/amls-art-market-participant-frontend/check-your-answers"

  private def generateRedirect(destinationUrl: String) = {
    Call(redirectCallType, destinationUrl)
  }

  def section(implicit cache: CacheMap): Section = {
    val notStarted = Section(key, NotStarted, false, generateRedirect(ampWhatYouNeedUrl))
    cache.getEntry[Amp](key).fold(notStarted) {
      model =>
        if (model.isComplete) {
          Section(key, Completed, model.hasChanged, generateRedirect(ampSummeryUrl))
        } else {
          Section(key, Started, model.hasChanged, generateRedirect(ampWhatYouNeedUrl))
        }
    }
  }

  implicit val mongoKey = new MongoKey[Amp] {
    override def apply(): String = key
  }

  implicit lazy val reads: Reads[Amp] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "_id").read[String] and
        (__ \ "data").read[JsObject] and
        (__ \ "lastUpdated").read(MongoDateTimeFormats.localDateTimeRead) and
        (__ \ "hasChanged").readNullable[Boolean].map(_.getOrElse(false)) and
        (__ \ "hasAccepted").readNullable[Boolean].map(_.getOrElse(false))
      ) (Amp.apply _)
  }

  implicit lazy val writes: OWrites[Amp] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "_id").write[String] and
        (__ \ "data").write[JsObject] and
        (__ \ "lastUpdated").write(MongoDateTimeFormats.localDateTimeWrite) and
        (__ \ "hasChanged").write[Boolean] and
        (__ \ "hasAccepted").write[Boolean]
      ) (unlift(Amp.unapply))
  }

  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
}
