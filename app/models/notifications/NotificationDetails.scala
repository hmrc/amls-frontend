/*
 * Copyright 2017 HM Revenue & Customs
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
import models.notifications.ContactType.{ApplicationAutorejectionForFailureToPay, DeRegistrationEffectiveDateChange, RegistrationVariationApproval}
import models.notifications.StatusType.DeRegistered
import org.joda.time.{DateTime, DateTimeZone, LocalDate}
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter, ISODateTimeFormat}
import play.api.libs.json._

import scala.util.matching.Regex

case class NotificationDetails(contactType: Option[ContactType],
                               status: Option[Status],
                               messageText: Option[String],
                               variation: Boolean,
                               receivedAt: DateTime) {

  def subject = s"notifications.subject.$getContactType"

  def dateReceived = {
    val fmt: DateTimeFormatter = DateTimeFormat.forPattern("d MMMM Y")
    receivedAt.toString(fmt)
  }

  private def getContactType: ContactType = {

    val statusReason = for {
      st <- status
      reason <- st.statusReason
    } yield reason

    contactType.getOrElse(
      (status, statusReason, variation) match {
        case (Some(Status(Some(DeRegistered), _)), _, _) => DeRegistrationEffectiveDateChange
        case (_, Some(_), _) => ApplicationAutorejectionForFailureToPay
        case (_, _, true) => RegistrationVariationApproval
        case _ => throw new RuntimeException("No matching ContactType found")
      }
    )
  }

}

object NotificationDetails {

  val dateTimeFormat = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC

  private val parseDate: String => LocalDate =
    input => LocalDate.parse(input, DateTimeFormat.forPattern("dd/MM/yyyy"))

  private val extractEndDate: String => Option[LocalDate] = input => {
    val pattern = """(?i)[\w\s]+-(\d{1,2}/\d{1,2}/\d{4})""".r.unanchored
    pattern.findFirstMatchIn(input).fold(none[LocalDate])(m => parseDate(m.group(1)).some)
  }

  private val extractReference: String => Option[String] = input => {
    val pattern = """(?i)[\w\s]+-([a-z][a-z0-9]+)""".r.unanchored
    pattern.findFirstMatchIn(input).fold(none[String])(m => m.group(1).some)
  }

  def convertEndDateWithRefMessageText(inputString: String): Option[EndDateDetails] = {
    for {
      date <- extractEndDate(inputString)
      ref <- extractReference(inputString)
    } yield EndDateDetails(date, ref.some)
  }

  def convertEndDateMessageText(inputString: String): Option[EndDateDetails] =
    extractEndDate(inputString) map { date => EndDateDetails(date, None) }

  def convertReminderMessageText(inputString: String): Option[ReminderDetails] = {
    inputString.split("\\|").toList match {
      case amount :: ref :: tail =>
        Some(ReminderDetails(Currency(splitByDash(amount).toDouble), splitByDash(ref)))
      case _ => None
    }
  }

  private def splitByDash(s: String): String = s.split("-")(1)

  implicit val dateTimeRead: Reads[DateTime] = {
    (__ \ "$date").read[Long].map { dateTime =>
      new DateTime(dateTime, DateTimeZone.UTC)
    }
  }

  implicit val reads = Json.reads[NotificationDetails]

}
