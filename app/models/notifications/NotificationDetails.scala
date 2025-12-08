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

package models.notifications

import cats.implicits._
import models.confirmation.Currency
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.custom.JsPathSupport.{localDateTimeReads, localDateTimeWrites}
import utils.ContactTypeHelper

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

case class NotificationDetails(
  contactType: Option[ContactType],
  status: Option[Status],
  messageText: Option[String],
  variation: Boolean,
  receivedAt: LocalDateTime
) {

  val cType: ContactType = ContactTypeHelper.getContactType(status, contactType, variation)

  def subject(templateVersion: String): String =
    templateVersion match {
      case "v1m0" | "v2m0" | "v3m0" | "v4m0" => s"notifications.subject.$cType"
      case "v5m0"                            => s"notifications.subject.v5.$cType"
      case "v6m0"                            => s"notifications.subject.v6.$cType"
      case "v7m0"                            => s"notifications.subject.v7.$cType"
      case _                                 => throw new Exception(s"Unknown template version $templateVersion")
    }

  def dateReceived: String =
    receivedAt.format(DateTimeFormatter.ofPattern("d MMMM Y"))
}

object NotificationDetails {
  val reads: Reads[NotificationDetails] =
    (
      (JsPath \ "contactType").readNullable[ContactType] and
        (JsPath \ "status").readNullable[Status] and
        (JsPath \ "messageText").readNullable[String] and
        (JsPath \ "variation").read[Boolean] and
        (JsPath \ "receivedAt").read[LocalDateTime](localDateTimeReads)
    )(NotificationDetails.apply _)

  val writes: OWrites[NotificationDetails] =
    (
      (JsPath \ "contactType").writeNullable[ContactType] and
        (JsPath \ "status").writeNullable[Status] and
        (JsPath \ "messageText").writeNullable[String] and
        (JsPath \ "variation").write[Boolean] and
        (JsPath \ "receivedAt").write[LocalDateTime](localDateTimeWrites)
    )(unlift(NotificationDetails.unapply))

  implicit val format: OFormat[NotificationDetails] = OFormat(reads, writes)

  private val parseDate: String => LocalDate =
    input => LocalDate.parse(input, DateTimeFormatter.ofPattern("dd/MM/yyyy"))

  private val extractEndDate: String => Option[LocalDate] = input => {
    val pattern = """(?i)[\w\s]+\s*-\s*(\d{1,2}/\d{1,2}/\d{4})""".r.unanchored
    pattern.findFirstMatchIn(input) match {
      case Some(m) =>
        val dateStr = m.group(1)
        try {
          val result = parseDate(dateStr)
          result.some
        } catch {
          case e: Exception =>
            none[LocalDate]
        }
      case None =>
        none[LocalDate]
    }
  }

  private val extractReference: String => Option[String] = input => {
    val pattern = """(?i)[\w\s]+\s*-\s*([a-z][a-z0-9]+)""".r.unanchored
    pattern.findFirstMatchIn(input).fold(none[String])(m => m.group(1).some)
  }

  def convertEndDateWithRefMessageText(inputString: String): Option[EndDateDetails] =
    for {
      date <- extractEndDate(inputString)
      ref  <- extractReference(inputString)
    } yield EndDateDetails(date, ref.some)

  def convertEndDateMessageText(inputString: String): Option[EndDateDetails] =
    extractEndDate(inputString) map { date => EndDateDetails(date, None) }

  def convertReminderMessageText(inputString: String, receivedAt: LocalDateTime): Option[ReminderDetails] =
    inputString.split("\\|").toList match {
      case amount :: ref :: tail =>
        val dueDate = receivedAt.plusDays(28).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
        Some(ReminderDetails(
          Currency(splitByDash(amount).toDouble),
          splitByDash(ref),
          dueDate
        ))
      case _ => None
    }

  def processGenericMessage(msg: String): String = {
    val pattern = """<!\[CDATA\[(.*)\]\]>""".r
    pattern.findFirstMatchIn(msg).fold(msg)(m => m.group(1))
  }

  def splitByDash(s: String): String = s.split("-")(1)
}
