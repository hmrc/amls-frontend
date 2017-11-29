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

package controllers

import javax.inject.{Inject, Singleton}

import cats.data.OptionT
import cats.implicits._
import config.ApplicationConfig
import connectors.{AmlsConnector, DataCacheConnector}
import models.notifications.ContactType._
import models.notifications._
import models.status.{SubmissionDecisionRejected, SubmissionStatus}
import play.api.mvc.Request
import services.{AuthEnrolmentsService, NotificationService, StatusService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{BusinessName, FeatureToggle}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class NotificationController @Inject()(
                                        val authEnrolmentsService: AuthEnrolmentsService,
                                        val statusService: StatusService,
                                        val authConnector: AuthConnector,
                                        val amlsNotificationService: NotificationService,
                                        implicit val amlsConnector: AmlsConnector,
                                        implicit val dataCacheConnector: DataCacheConnector
                                      ) extends BaseController {

  def getMessages = Authorised.async {
    implicit authContext =>
      implicit request =>
        statusService.getReadStatus flatMap {
          case readStatus if readStatus.safeId.isDefined =>
            (for {
              businessName <- BusinessName.getName(readStatus.safeId)
              records <- OptionT.liftF(amlsNotificationService.getNotifications(readStatus.safeId.get))
            } yield {
              Ok(views.html.notifications.your_messages(businessName, records))
            }) getOrElse (throw new Exception("Cannot retrieve business name"))
          case _ => throw new Exception("Unable to retrieve SafeID")
        }
  }

  def messageDetails(id: String, contactType: ContactType, amlsRegNo: String) = Authorised.async {
    implicit authContext =>
      implicit request =>
        statusService.getReadStatus flatMap {
          case readStatus if readStatus.safeId.isDefined =>
            (for {
              safeId <- OptionT.fromOption[Future](readStatus.safeId)
              businessName <- BusinessName.getName(readStatus.safeId)
              details <- OptionT(amlsNotificationService.getMessageDetails(amlsRegNo, id, contactType))
              status <- OptionT.liftF(statusService.getStatus)
            } yield contactTypeToResponse(contactType, (amlsRegNo, safeId), businessName, details, status)) getOrElse NotFound(notFoundView)
          case r if r.safeId.isEmpty => throw new Exception("Unable to retrieve SafeID")
          case _ => Future.successful(BadRequest)
        }
  }

  private def contactTypeToResponse(
                                     contactType: ContactType,
                                     reference: (String, String),
                                     businessName: String,
                                     details: NotificationDetails,
                                     status: SubmissionStatus)(implicit request: Request[_]) = {

    val msgText = details.messageText.getOrElse("")

    val (amlsRefNo, safeId) = reference

    contactType match {
      case MindedToRevoke => Ok(views.html.notifications.minded_to_revoke(msgText, amlsRefNo, businessName))
      case MindedToReject => Ok(views.html.notifications.minded_to_reject(msgText, safeId, businessName))
      case RejectionReasons => Ok(views.html.notifications.rejection_reasons(msgText, safeId, businessName, details.dateReceived))
      case RevocationReasons => Ok(views.html.notifications.revocation_reasons(msgText, amlsRefNo, businessName, details.dateReceived))
      case NoLongerMindedToReject => Ok(views.html.notifications.no_longer_minded_to_reject(msgText, safeId))
      case NoLongerMindedToRevoke => Ok(views.html.notifications.no_longer_minded_to_revoke(msgText, amlsRefNo))
      case _ =>
        (status, contactType) match {
          case (SubmissionDecisionRejected, _) | (_, DeRegistrationEffectiveDateChange) =>
            Ok(views.html.notifications.message_details(details.subject, msgText, safeId.some))
          case _ =>
            Ok(views.html.notifications.message_details(details.subject, msgText, None))
        }
    }
  }
}