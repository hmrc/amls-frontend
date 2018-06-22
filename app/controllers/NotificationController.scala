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

package controllers

import cats.data.OptionT
import cats.implicits._
import connectors.{AmlsConnector, DataCacheConnector}
import javax.inject.{Inject, Singleton}
import models.notifications.ContactType._
import models.notifications._
import models.status.{SubmissionDecisionRejected, SubmissionStatus}
import play.api.mvc.{Request, Result}
import services.businessmatching.BusinessMatchingService
import services.{AuthEnrolmentsService, NotificationService, StatusService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.BusinessName

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class NotificationController @Inject()(
                                        val authEnrolmentsService: AuthEnrolmentsService,
                                        val statusService: StatusService,
                                        val businessMatchingService: BusinessMatchingService,
                                        val authConnector: AuthConnector,
                                        val amlsNotificationService: NotificationService,
                                        implicit val amlsConnector: AmlsConnector,
                                        implicit val dataCacheConnector: DataCacheConnector
                                      ) extends BaseController {

  def getMessages = Authorised.async {
    implicit authContext =>
      implicit request =>
        authEnrolmentsService.amlsRegistrationNumber flatMap {
          case Some(mlrRegNumber) => {
              statusService.getReadStatus(mlrRegNumber) flatMap {
                case readStatus if readStatus.safeId.isDefined => generateNotificationView(readStatus.safeId.get, Some(mlrRegNumber))
                case _ => throw new Exception("Unable to retrieve SafeID")
              }
            }
          case _ => {
              businessMatchingService.getModel.value flatMap {
                case Some(model) if model.reviewDetails.isDefined => generateNotificationView(model.reviewDetails.get.safeId, None)
                case _ => throw new Exception("Unable to retrieve SafeID from reviewDetails")
              }
            }
        }
  }

  private def generateNotificationView(safeId: String, refNumber: Option[String])
                                      (implicit hc: HeaderCarrier, authContext: AuthContext, request: Request[_]): Future[Result] = {
    (for {
      businessName <- BusinessName.getName(Some(safeId))
      records: Seq[NotificationRow] <- OptionT.liftF(amlsNotificationService.getNotifications(safeId))
    } yield {
      val currentRecords: Seq[NotificationRow] = (for {
        amls <- refNumber
      } yield records.filter(_.amlsRegistrationNumber == amls)) getOrElse records
      val previousRecords: Seq[NotificationRow] = (for {
        amls <- refNumber
      } yield records.filter(_.amlsRegistrationNumber != amls)) getOrElse Seq()
      Ok(views.html.notifications.your_messages(businessName, currentRecords, previousRecords))
    }) getOrElse (throw new Exception("Cannot retrieve business name"))
  }

  def messageDetails(id: String, contactType: ContactType, amlsRegNo: String) = Authorised.async {
    implicit authContext =>
      implicit request =>
        statusService.getReadStatus(amlsRegNo) flatMap {
          case readStatus if readStatus.safeId.isDefined =>
            (for {
              safeId <- OptionT.fromOption[Future](readStatus.safeId)
              businessName <- BusinessName.getName(readStatus.safeId)
              details <- OptionT(amlsNotificationService.getMessageDetails(amlsRegNo, id, contactType))
              status <- OptionT.liftF(statusService.getStatus(amlsRegNo))
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