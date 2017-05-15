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

import models.confirmation.Currency
import models.notifications.ContactType.{ApplicationAutorejectionForFailureToPay, DeRegistrationEffectiveDateChange, RegistrationVariationApproval}
import models.notifications.StatusType.DeRegistered
import org.joda.time.LocalDate
import org.joda.time.format.{DateTimeFormat, ISODateTimeFormat}
import play.api.libs.json.Json

case class NotificationDetails(contactType: Option[ContactType],
                               status: Option[Status],
                               messageText: Option[String],
                               variation: Boolean) {

  def subject = {
    s"notifications.subject.$getContactType"
  }

  private def getContactType: ContactType = {

    val statusReason = for {
      st <- status
      reason <- st.statusReason
    } yield reason

    contactType.getOrElse(
      (status, statusReason, variation) match {
        case (Some(Status(Some(DeRegistered), _)), _, _) => DeRegistrationEffectiveDateChange
        case (_, Some(r), _) => ApplicationAutorejectionForFailureToPay
        case (_, _, true) => RegistrationVariationApproval
        case _ => throw new RuntimeException("No matching ContactType found")
      }
    )
  }
}

object NotificationDetails {

  val dateTimeFormat = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC

  def convertEndDateWithRefMessageText(inputString: String): Option[EndDateDetails] = {

    inputString.split("\\|").toList match {
      case date :: ref :: Nil => {
        val dateValue = LocalDate.parse(splitByDash(date), DateTimeFormat.forPattern("dd/MM/yyyy"))
        Some(EndDateDetails(dateValue, Some(splitByDash(ref))))
      }
      case _ => None
    }
  }

  def convertEndDateMessageText(inputString: String): Option[EndDateDetails] = {

    inputString.split("-").toList match {
      case label :: date :: Nil => {
        val dateValue = LocalDate.parse(splitByDash(inputString), DateTimeFormat.forPattern("dd/MM/yyyy"))
        Some(EndDateDetails(dateValue, None))
      }
      case _ => None
    }
  }

  def convertReminderMessageText(inputString: String): Option[ReminderDetails] = {
    inputString.split("\\|").toList match {
      case amount :: ref :: tail =>
        Some(ReminderDetails(Currency(splitByDash(amount).toDouble), splitByDash(ref)))
      case _ => None
    }
  }

  private def splitByDash(s: String): String = s.split("-")(1)

  implicit val reads = Json.reads[NotificationDetails]
}
