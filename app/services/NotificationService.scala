/*
 * Copyright 2018 HM Revenue & Customs
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

package services

import javax.inject.{Inject, Singleton}

import cats.data.OptionT
import cats.implicits._
import connectors.AmlsNotificationConnector
import models.notifications.{ContactType, NotificationDetails, NotificationRow}
import play.api.i18n.MessagesApi
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class NotificationService @Inject()(val amlsNotificationConnector: AmlsNotificationConnector, val messagesApi: MessagesApi) {

  def getNotifications(safeId: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[Seq[NotificationRow]] =
    amlsNotificationConnector.fetchAllBySafeId(safeId) map {
      case notifications@(_::_) => notifications.sortWith((x, y) => x.receivedAt.isAfter(y.receivedAt))
      case notifications => notifications
    }

  def getMessageDetails(amlsRegNo: String, id: String, contactType: ContactType)
                       (implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[NotificationDetails]] = {

    contactType match {

      case ContactType.ApplicationAutorejectionForFailureToPay |
           ContactType.RegistrationVariationApproval |
           ContactType.DeRegistrationEffectiveDateChange => handleStaticMessage(amlsRegNo, id, contactType)

      case ContactType.ReminderToPayForVariation |
           ContactType.ReminderToPayForRenewal |
           ContactType.ReminderToPayForApplication |
           ContactType.ReminderToPayForManualCharges => handleReminderMessage(amlsRegNo, id, contactType)

      case ContactType.ApplicationApproval => handleEndDateWithRefMessage(amlsRegNo, id, contactType)

      case ContactType.RenewalApproval |
           ContactType.AutoExpiryOfRegistration |
           ContactType.RenewalReminder => handleEndDateMessage(amlsRegNo, id, contactType)

      case _ => (for {
        details <- OptionT(amlsNotificationConnector.getMessageDetailsByAmlsRegNo(amlsRegNo, id))
        messageText <- OptionT.fromOption[Future](details.messageText)
      } yield details.copy(messageText = Some(CustomAttributeProvider.commonMark(NotificationDetails.processGenericMessage(messageText))))).value
    }
  }

  private def handleStaticMessage(amlsRegNo: String, id: String, contactType: ContactType)
                                 (implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[NotificationDetails]] = {

    amlsNotificationConnector.getMessageDetailsByAmlsRegNo(amlsRegNo, id) map {
      case Some(notificationDetails) => {
        Some(NotificationDetails(
          Some(contactType),
          None,
          Some(
            messagesApi(s"notification.static.text.$contactType",
              controllers.routes.StatusController.get())
          ),
          false,
          notificationDetails.receivedAt
        ))
      }
      case _ => None
    }

  }

  private def handleReminderMessage(amlsRegNo: String, id: String, contactType: ContactType)
                                   (implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[NotificationDetails]] = {

    amlsNotificationConnector.getMessageDetailsByAmlsRegNo(amlsRegNo, id) map {
      case Some(notificationDetails) => {
        for {
          message <- notificationDetails.messageText
          details <- NotificationDetails.convertReminderMessageText(message)
        } yield {
          notificationDetails.copy(messageText = Some(messagesApi(
            s"notification.reminder.to.pay.$contactType",
            details.paymentAmount,
            details.referenceNumber
          )))
        }
      }
      case _ => None
    }
  }

  private def handleEndDateMessage(amlsRegNo: String, id: String, contactType: ContactType)
                                  (implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[NotificationDetails]] = {

    amlsNotificationConnector.getMessageDetailsByAmlsRegNo(amlsRegNo, id) map {
      case Some(notificationDetails) => {
        for {
          message <- notificationDetails.messageText
          details <- NotificationDetails.convertEndDateMessageText(message)
        } yield {
          notificationDetails.copy(messageText = Some(messagesApi(
            s"notification.message.with.end.date.$contactType",
            details.endDate
          )))
        }
      }
      case _ => None
    }
  }

  private def handleEndDateWithRefMessage(amlsRegNo: String, id: String, contactType: ContactType)
                                         (implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[NotificationDetails]] = {

    amlsNotificationConnector.getMessageDetailsByAmlsRegNo(amlsRegNo, id) map {
      case Some(notificationDetails) => {
        for {
          message <- notificationDetails.messageText
          details <- NotificationDetails.convertEndDateWithRefMessageText(message)
        } yield {
          notificationDetails.copy(messageText = Some(messagesApi(
            s"notification.message.with.end.date.$contactType",
            details.endDate,
            details.referenceNumber.getOrElse("")
          )))
        }
      }
      case _ => None
    }
  }
}
