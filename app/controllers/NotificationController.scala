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
import play.api.i18n.Messages
import play.api.mvc.{Request, Result}
import play.twirl.api.Template3
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

  def messageDetails(id: String, contactType: ContactType, amlsRegNo: String, templateVersion: String) = Authorised.async {
    implicit authContext =>
      implicit request =>
        statusService.getReadStatus(amlsRegNo) flatMap {
          case readStatus if readStatus.safeId.isDefined =>
            (for {
              safeId <- OptionT.fromOption[Future](readStatus.safeId)
              businessName <- BusinessName.getName(readStatus.safeId)
              details <- OptionT(amlsNotificationService.getMessageDetails(amlsRegNo, id, contactType, templateVersion))
              status <- OptionT.liftF(statusService.getStatus(amlsRegNo))
            } yield contactTypeToResponse(contactType, (amlsRegNo, safeId), businessName, details, status, templateVersion)) getOrElse NotFound(notFoundView)
          case r if r.safeId.isEmpty => throw new Exception("Unable to retrieve SafeID")
          case _ => Future.successful(BadRequest)
        }
  }

  private def contactTypeToResponse(
                                     contactType: ContactType,
                                     reference: (String, String),
                                     businessName: String,
                                     details: NotificationDetails,
                                     status: SubmissionStatus,
                                     templateVersion: String)(implicit request: Request[_], m: Messages) = {

    val msgText = details.messageText.getOrElse("")

    val (amlsRefNo, safeId) = reference

    def getTemplate[T](name : String)(implicit man: Manifest[T]) : T =
      Class.forName(name + "$").getField("MODULE$").get(man.runtimeClass).asInstanceOf[T]

    def render(templateName: String, notificationParams: NotificationParams, templateVersion: String) =
      getTemplate[Template3[NotificationParams, Request[_], Messages, play.twirl.api.Html]](s"views.html.notifications.${ templateVersion }.${ templateName }")
        .render(notificationParams, request, m)

    val notification = contactType match {
      case MindedToRevoke => render("minded_to_revoke", NotificationParams(
        msgContent = msgText, amlsRefNo = Some(amlsRefNo), businessName = Some(businessName)), templateVersion)

      case MindedToReject => render("minded_to_reject", NotificationParams(
        msgContent = msgText, safeId = Some(safeId), businessName = Some(businessName)), templateVersion)

      case RejectionReasons => render("rejection_reasons", NotificationParams(
        msgContent = msgText, safeId = Some(safeId), businessName = Some(businessName), endDate = Some(details.dateReceived)), templateVersion)

      case RevocationReasons => render("revocation_reasons", NotificationParams(
        msgContent = msgText, amlsRefNo = Some(amlsRefNo), businessName = Some(businessName), endDate = Some(details.dateReceived)), templateVersion)

      case NoLongerMindedToReject => render("no_longer_minded_to_reject", NotificationParams(
        msgContent = msgText, safeId = Some(safeId)), templateVersion)

      case NoLongerMindedToRevoke => render("no_longer_minded_to_revoke", NotificationParams(
        msgContent = msgText, amlsRefNo = Some(amlsRefNo)), templateVersion)

      case _ =>
        (status, contactType) match {
          case (SubmissionDecisionRejected, _) | (_, DeRegistrationEffectiveDateChange) => {
            render("message_details", NotificationParams(
              msgTitle = details.subject, msgContent = msgText, safeId = safeId.some), templateVersion)
          }
          case _ =>
            render("message_details", NotificationParams(
              msgTitle = details.subject, msgContent = msgText), templateVersion)
        }
    }
    Ok(notification)
  }
}