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
import models.registrationprogress.Section
import play.api.libs.json._
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap

final case class Amp(
                       id: String,
                       data: JsObject = Json.obj()
                     ) {

  //TODO add a setter for 'data' here. Check if it has changed and set a changed flag if different (as per hvd)

  val typeOfParticipantPath            = JsPath \ "typeOfParticipant"
  val typeOfParticipantDetailPath      = JsPath \ "typeOfParticipantDetail"
  val boughtOrSoldOverThresholdPath    = JsPath \ "boughtOrSoldOverThreshold"
  val identifyLinkedTransactionsPath   = JsPath \ "identifyLinkedTransactions"
  val dateTransactionOverThresholdPath = JsPath \ "dateTransactionOverThreshold"
  val percentageExpectedTurnoverPath   = JsPath \ "percentageExpectedTurnover"


  private def get[A](path: JsPath)(implicit rds: Reads[A]): Option[A] =
    Reads.optionNoError(Reads.at(path)).reads(data).getOrElse(None)

  private def isTypeOfParticipantComplete: Boolean = {
    get[JsValue](typeOfParticipantPath).isDefined &&
      ((get[JsValue](typeOfParticipantPath).getOrElse(Seq()).toString().contains("somethingElse") &&
        get[JsValue](typeOfParticipantDetailPath).isDefined) ||
        (!get[JsValue](typeOfParticipantPath).getOrElse(Seq()).toString().contains("somethingElse")))
  }

  private def isBoughtOrSoldOverThresholdComplete: Boolean = {
    get[JsValue](boughtOrSoldOverThresholdPath).isDefined &&
      ((get[JsValue](boughtOrSoldOverThresholdPath).getOrElse(false).toString() == "true" &&
        get[JsValue](dateTransactionOverThresholdPath).isDefined) ||
        (get[JsValue](boughtOrSoldOverThresholdPath).getOrElse(false).toString() == "false"))
  }

  def isComplete: Boolean = {
    isTypeOfParticipantComplete &&
    isBoughtOrSoldOverThresholdComplete &&
    get[JsValue](identifyLinkedTransactionsPath).isDefined &&
    get[JsValue](percentageExpectedTurnoverPath).isDefined
  }
}

object Amp {

  //TODO - Section def yet to be implemented
  def section(implicit cache: CacheMap): Section = ???

  val key = "amp"

  implicit val mongoKey = new MongoKey[Amp] {
    override def apply(): String = "amp"
  }

  implicit lazy val reads: Reads[Amp] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "_id").read[String] and
        (__ \ "data").read[JsObject]
      ) (Amp.apply _)
  }

  implicit lazy val writes: OWrites[Amp] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "_id").write[String] and
        (__ \ "data").write[JsObject]
      ) (unlift(Amp.unapply))
  }
}
