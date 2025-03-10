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

package models.amp

import config.ApplicationConfig
import models.registrationprogress._
import models.renewal.AMPTurnover
import play.api.i18n.Messages
import play.api.libs.json._
import play.api.mvc.Call
import typeclasses.MongoKey
import services.cache.Cache

/** Art Market Participant (Amp).
  */
final case class Amp(data: JsObject = Json.obj(), hasChanged: Boolean = false, hasAccepted: Boolean = false) {

  /** Provides a means of setting data that will update the hasChanged flag
    *
    * Set data via this method and NOT directly in the constructor
    */
  def data(p: JsObject): Amp =
    this.copy(data = p, hasChanged = hasChanged || this.data != p, hasAccepted = hasAccepted && this.data == p)

  val typeOfParticipant: JsPath            = JsPath \ "typeOfParticipant"
  val typeOfParticipantDetail: JsPath      = JsPath \ "typeOfParticipantDetail"
  val soldOverThreshold: JsPath            = JsPath \ "soldOverThreshold"
  val identifyLinkedTransactions: JsPath   = JsPath \ "identifyLinkedTransactions"
  val dateTransactionOverThreshold: JsPath = JsPath \ "dateTransactionOverThreshold"
  val percentageExpectedTurnover: JsPath   = JsPath \ "percentageExpectedTurnover"
  val otherTypeOfParticipant               = "somethingelse"
  val notPresent                           = "null"

  private def get[A](path: JsPath)(implicit rds: Reads[A]): Option[A] =
    Reads.optionNoError(Reads.at(path)).reads(data).getOrElse(None)

  private def valueAt(path: JsPath): String =
    get[JsValue](path).getOrElse(notPresent).toString.toLowerCase()

  private def isTypeOfParticipantComplete: Boolean =
    valueAt(typeOfParticipant) != notPresent &&
      ((valueAt(typeOfParticipant).contains(otherTypeOfParticipant) &&
        valueAt(typeOfParticipantDetail) != notPresent) ||
        (!valueAt(typeOfParticipant).contains(otherTypeOfParticipant)))

  private def isSoldOverThresholdComplete: Boolean =
    valueAt(soldOverThreshold) != notPresent &&
      ((valueAt(soldOverThreshold) == "true" &&
        valueAt(dateTransactionOverThreshold) != notPresent) ||
        (valueAt(soldOverThreshold) == "false"))

  def isComplete: Boolean =
    isTypeOfParticipantComplete &&
      isSoldOverThresholdComplete &&
      valueAt(identifyLinkedTransactions) != notPresent &&
      valueAt(percentageExpectedTurnover) != notPresent
}

object Amp {
  val redirectCallType = "GET"
  val key              = "amp"

  private def generateRedirect(destinationUrl: String) =
    Call(redirectCallType, destinationUrl)

  def taskRow(appConfig: ApplicationConfig)(implicit cache: Cache, messages: Messages): TaskRow = {
    val notStarted = TaskRow(
      key,
      generateRedirect(appConfig.ampWhatYouNeedUrl).url,
      hasChanged = false,
      NotStarted,
      TaskRow.notStartedTag
    )
    cache.getEntry[Amp](key).fold(notStarted) { model =>
      if (model.isComplete && model.hasAccepted && model.hasChanged) {
        TaskRow(
          key,
          generateRedirect(appConfig.ampSummaryUrl).url,
          hasChanged = true,
          status = Updated,
          tag = TaskRow.updatedTag
        )
      } else if (model.isComplete && model.hasAccepted) {
        TaskRow(
          key,
          generateRedirect(appConfig.ampSummaryUrl).url,
          model.hasChanged,
          Completed,
          TaskRow.completedTag
        )
      } else {
        TaskRow(
          key,
          generateRedirect(appConfig.ampWhatYouNeedUrl).url,
          model.hasChanged,
          Started,
          TaskRow.incompleteTag
        )
      }
    }
  }

  implicit val mongoKey: MongoKey[Amp] = () => key

  implicit lazy val reads: Reads[Amp] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "data").read[JsObject] and
        (__ \ "hasChanged").readNullable[Boolean].map(_.getOrElse(false)) and
        (__ \ "hasAccepted").readNullable[Boolean].map(_.getOrElse(false))
    )(Amp.apply _)
  }

  implicit lazy val writes: OWrites[Amp] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "data").write[JsObject] and
        (__ \ "hasChanged").write[Boolean] and
        (__ \ "hasAccepted").write[Boolean]
    )(unlift(Amp.unapply))
  }

  implicit val formatOption: Reads[Option[Amp]] = Reads.optionWithNull[Amp]

  def convert(model: Amp): AMPTurnover =
    model.get[JsValue](model.percentageExpectedTurnover) match {
      case Some(JsString("zeroToTwenty"))          => AMPTurnover.First
      case Some(JsString("twentyOneToForty"))      => AMPTurnover.Second
      case Some(JsString("fortyOneToSixty"))       => AMPTurnover.Third
      case Some(JsString("sixtyOneToEighty"))      => AMPTurnover.Fourth
      case Some(JsString("eightyOneToOneHundred")) => AMPTurnover.Fifth
      case _                                       => throw new Exception("Incorrect data field from AMP turnover")
    }
}
