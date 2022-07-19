/*
 * Copyright 2022 HM Revenue & Customs
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

import cats.data.OptionT
import cats.implicits._
import connectors.AmlsNotificationConnector
import javax.inject.{Inject, Singleton}
import models.notifications.{ContactType, NotificationDetails, NotificationRow}
import play.api.i18n._
import uk.gov.hmrc.http.HeaderCarrier
import scala.language.reflectiveCalls
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NotificationService @Inject()(val amlsNotificationConnector: AmlsNotificationConnector, val messagesApi: MessagesApi) {

  def getNotifications(safeId: String, accountTypeId: (String, String))(implicit hc: HeaderCarrier ,ec: ExecutionContext): Future[Seq[NotificationRow]] =
    amlsNotificationConnector.fetchAllBySafeId(safeId, accountTypeId) map {
      case notifications@(_::_) =>
        notifications.sortWith((x, y) => x.receivedAt.isAfter(y.receivedAt))
      case notifications => notifications
    }

  def getMessageDetails(amlsRegNo: String, id: String, contactType: ContactType, templateVersion: String, accountTypeId: (String, String))
                       (implicit hc: HeaderCarrier ,ec: ExecutionContext): Future[Option[NotificationDetails]] = {

    contactType match {

      case ContactType.ApplicationAutorejectionForFailureToPay |
           ContactType.RegistrationVariationApproval |
           ContactType.DeRegistrationEffectiveDateChange => handleStaticMessage(amlsRegNo, id, contactType, templateVersion, accountTypeId)

      case ContactType.ReminderToPayForVariation |
           ContactType.ReminderToPayForRenewal |
           ContactType.ReminderToPayForApplication |
           ContactType.ReminderToPayForManualCharges => handleReminderMessage(amlsRegNo, id, contactType, templateVersion, accountTypeId)

      case ContactType.ApplicationApproval => handleEndDateWithRefMessage(amlsRegNo, id, contactType, templateVersion, accountTypeId)

      case ContactType.RenewalApproval |
           ContactType.AutoExpiryOfRegistration |
           ContactType.RenewalReminder |
           ContactType.NewRenewalReminder => handleEndDateMessage(amlsRegNo, id, contactType, templateVersion, accountTypeId)

      case _ => (for {
        details <- OptionT(amlsNotificationConnector.getMessageDetailsByAmlsRegNo(amlsRegNo, id, accountTypeId))
        messageText <- OptionT.fromOption[Future](details.messageText match {
          case t@Some(_) => t
          case _ => Some("<![CDATA[<P>No content</P>]]>")
        })
      } yield details.copy(messageText = Some(CustomAttributeProvider.commonMark(NotificationDetails.processGenericMessage(messageText))))).value
    }
  }

  private def handleStaticMessage(amlsRegNo: String, id: String, contactType: ContactType, templateVersion: String, accountTypeId: (String, String))
                                 (implicit hc: HeaderCarrier ,ec: ExecutionContext): Future[Option[NotificationDetails]] = {

    val staticMessage = Class.forName(s"services.notifications.${ templateVersion }.MessageDetails")
      .getDeclaredConstructor().newInstance().asInstanceOf[{ def static(contactType: ContactType, url: String): String }]

    amlsNotificationConnector.getMessageDetailsByAmlsRegNo(amlsRegNo, id, accountTypeId) map {
      case Some(notificationDetails) => {
        Some(NotificationDetails(
          Some(contactType),
          None,
          Some(staticMessage.static(contactType, controllers.routes.StatusController.get().url)),
          false,
          notificationDetails.receivedAt
        ))
      }
      case _ => None
    }

  }

  private def handleReminderMessage(amlsRegNo: String, id: String, contactType: ContactType, templateVersion: String, accountTypeId: (String, String))
                                   (implicit hc: HeaderCarrier ,ec: ExecutionContext): Future[Option[NotificationDetails]] = {

    val reminderMessage = Class.forName(s"services.notifications.${ templateVersion }.MessageDetails")
      .getDeclaredConstructor().newInstance().asInstanceOf[{ def reminder(contactType: ContactType, paymentAmount: String, referenceNumber: String): String }]

    amlsNotificationConnector.getMessageDetailsByAmlsRegNo(amlsRegNo, id, accountTypeId) map {
      case Some(notificationDetails) => {
        for {
          message <- notificationDetails.messageText
          details <- NotificationDetails.convertReminderMessageText(message)
        } yield {
          notificationDetails.copy(messageText = Some(reminderMessage.reminder(
            contactType,
            details.paymentAmount.toString,
            details.referenceNumber
          )))
        }
      }
      case _ => None
    }
  }

  private def handleEndDateMessage(amlsRegNo: String, id: String, contactType: ContactType, templateVersion: String, accountTypeId: (String, String))
                                  (implicit hc: HeaderCarrier ,ec: ExecutionContext): Future[Option[NotificationDetails]] = {

    val endDateMessage = Class.forName(s"services.notifications.${ templateVersion }.MessageDetails")
      .getDeclaredConstructor().newInstance().asInstanceOf[{ def endDate(contactType: ContactType, endDate: String, url: String, referenceNumber: String): String }]

    amlsNotificationConnector.getMessageDetailsByAmlsRegNo(amlsRegNo, id, accountTypeId) map {
      case Some(notificationDetails) => {
        for {
          message <- notificationDetails.messageText
          details <- NotificationDetails.convertEndDateMessageText(message)
        } yield {

          val endDate: String =
          if(templateVersion == "v5m0") details.endDate.toString("dd-MM-yyyy") else details.endDate.toString

          notificationDetails.copy(messageText = Some(endDateMessage.endDate(
            contactType,
            endDate,
            controllers.routes.StatusController.get().url,
            ""
          )))
        }
      }
      case _ => None
    }
  }

  private def handleEndDateWithRefMessage(amlsRegNo: String, id: String, contactType: ContactType, templateVersion: String, accountTypeId: (String, String))
                                         (implicit hc: HeaderCarrier ,ec: ExecutionContext): Future[Option[NotificationDetails]] = {

    val endDateMessage = Class.forName(s"services.notifications.${ templateVersion }.MessageDetails")
      .getDeclaredConstructor().newInstance().asInstanceOf[{ def endDate(contactType: ContactType, endDate: String, url: String, referenceNumber: String): String }]

    amlsNotificationConnector.getMessageDetailsByAmlsRegNo(amlsRegNo, id, accountTypeId) map {
      case Some(notificationDetails) => {
        for {
          message <- notificationDetails.messageText
          details <- NotificationDetails.convertEndDateWithRefMessageText(message)
        } yield {
          notificationDetails.copy(messageText = Some(endDateMessage.endDate(
            contactType,
            details.endDate.toString,
            controllers.routes.StatusController.get().url,
            details.referenceNumber.getOrElse("")
          )))
        }
      }
      case _ => None
    }
  }
}
