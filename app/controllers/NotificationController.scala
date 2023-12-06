/*
 * Copyright 2023 HM Revenue & Customs
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
import config.ApplicationConfig
import connectors.{AmlsConnector, DataCacheConnector}
import models.notifications.ContactType._
import models.notifications._
import models.status.{SubmissionDecisionRejected, SubmissionStatus}
import play.api.i18n.{Lang, Messages}
import play.api.mvc.{MessagesControllerComponents, Request, Result}
import play.twirl.api.Template5
import services.businessmatching.BusinessMatchingService
import services.{AuthEnrolmentsService, NotificationService, StatusService}
import uk.gov.hmrc.govukfrontend.views.Aliases.Table
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, BusinessName}
import views.html.notifications.YourMessagesView
import views.notifications._

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class NotificationController @Inject()(val authEnrolmentsService: AuthEnrolmentsService,
                                       val statusService: StatusService,
                                       val businessMatchingService: BusinessMatchingService,
                                       authAction: AuthAction,
                                       val ds: CommonPlayDependencies,
                                       val amlsNotificationService: NotificationService,
                                       implicit val amlsConnector: AmlsConnector,
                                       implicit val dataCacheConnector: DataCacheConnector,
                                       val cc: MessagesControllerComponents,
                                       view: YourMessagesView,
                                       implicit val error: views.html.ErrorView,
                                       v1m0: V1M0,
                                       v2m0: V2M0,
                                       v3m0: V3M0,
                                       v4m0: V4M0,
                                       v5m0: V5M0
                                      ) extends AmlsBaseController(ds, cc) {

  def notificationsMap(templateVersion: String, templateName: String) = {
    templateVersion match {
      case "v1m0" => version1Notifications(templateName)
      case "v2m0" => version2Notifications(templateName)
      case "v3m0" => version3Notifications(templateName)
      case "v4m0" => version4Notifications(templateName)
      case "v5m0" => version5Notifications(templateName)
      case _ => throw new RuntimeException(s"Notification version $templateVersion not found")
    }
  }

  def version1Notifications(templateName: String) = {
    templateName match {
      case "message_details" => v1m0.messageDetailsView
      case "minded_to_reject" => v1m0.mindedToRejectView
      case "minded_to_revoke" => v1m0.mindedToRevokeView
      case "no_longer_minded_to_reject" => v1m0.noLongerMindedToRejectView
      case "no_longer_minded_to_revoke" => v1m0.noLongerMindedToRevokeView
      case "rejection_reasons" => v1m0.rejectionReasonsView
      case "revocation_reasons" => v1m0.revocationReasonsView
      case _ => throw new RuntimeException(s"Message template $templateName not found")
    }
  }

  def version2Notifications(templateName: String) = {
    templateName match {
      case "message_details" => v2m0.messageDetailsView
      case "minded_to_reject" => v2m0.mindedToRejectView
      case "minded_to_revoke" => v2m0.mindedToRevokeView
      case "no_longer_minded_to_reject" => v2m0.noLongerMindedToRejectView
      case "no_longer_minded_to_revoke" => v2m0.noLongerMindedToRevokeView
      case "rejection_reasons" => v2m0.rejectionReasonsView
      case "revocation_reasons" => v2m0.revocationReasonsView
      case _ => throw new RuntimeException(s"Message template $templateName not found")
    }
  }

  def version3Notifications(templateName: String) = {
    templateName match {
      case "message_details" => v3m0.messageDetailsView
      case "minded_to_reject" => v3m0.mindedToRejectView
      case "minded_to_revoke" => v3m0.mindedToRevokeView
      case "no_longer_minded_to_reject" => v3m0.noLongerMindedToRejectView
      case "no_longer_minded_to_revoke" => v3m0.noLongerMindedToRevokeView
      case "rejection_reasons" => v3m0.rejectionReasonsView
      case "revocation_reasons" => v3m0.revocationReasonsView
      case _ => throw new RuntimeException(s"Message template $templateName not found")
    }
  }

  def version4Notifications(templateName: String) = {
    templateName match {
      case "message_details" => v4m0.messageDetailsView
      case "minded_to_reject" => v4m0.mindedToRejectView
      case "minded_to_revoke" => v4m0.mindedToRevokeView
      case "no_longer_minded_to_reject" => v4m0.noLongerMindedToRejectView
      case "no_longer_minded_to_revoke" => v4m0.noLongerMindedToRevokeView
      case "rejection_reasons" => v4m0.rejectionReasonsView
      case "revocation_reasons" => v4m0.revocationReasonsView
      case _ => throw new RuntimeException(s"Message template $templateName not found")
    }
  }

  def version5Notifications(templateName: String) = {
    templateName match {
      case "message_details" => v5m0.messageDetailsView
      case "minded_to_reject" => v5m0.mindedToRejectView
      case "minded_to_revoke" => v5m0.mindedToRevokeView
      case "no_longer_minded_to_reject" => v5m0.noLongerMindedToRejectView
      case "no_longer_minded_to_revoke" => v5m0.noLongerMindedToRevokeView
      case "rejection_reasons" => v5m0.rejectionReasonsView
      case "revocation_reasons" => v5m0.revocationReasonsView
      case _ => throw new RuntimeException(s"Message template $templateName not found")
    }
  }

  def getMessages = authAction.async {
      implicit request =>
        request.amlsRefNumber match {
          case Some(mlrRegNumber) => {
              statusService.getReadStatus(mlrRegNumber, request.accountTypeId) flatMap {
                case readStatus if readStatus.safeId.isDefined =>
                  generateNotificationView(request.credId, readStatus.safeId.get, Some(mlrRegNumber), request.accountTypeId)
                case _ => throw new Exception("Unable to retrieve SafeID")
              }
            }
          case _ => {
              businessMatchingService.getModel(request.credId).value flatMap {
                case Some(model) if model.reviewDetails.isDefined =>
                  generateNotificationView(request.credId, model.reviewDetails.get.safeId, None, request.accountTypeId)
                case _ => throw new Exception("Unable to retrieve SafeID from reviewDetails")
              }
            }
        }
  }

  private def generateNotificationView(credId: String, safeId: String, refNumber: Option[String], accountTypeId: (String, String))
                                      (implicit hc: HeaderCarrier, request: Request[_]): Future[Result] = {
    import models.notifications.{Status => NStatus}
    import org.joda.time.{DateTime, DateTimeZone}

    (for {
      businessName <- BusinessName.getName(credId, Some(safeId), accountTypeId)
      records: Seq[NotificationRow] <- OptionT.liftF(amlsNotificationService.getNotifications(safeId, accountTypeId))
    } yield {
      val currentRecordsWithIndexes = (for {
        amls <- refNumber
      } yield records.zipWithIndex.filter(_._1.amlsRegistrationNumber == amls)) getOrElse records.zipWithIndex
      val previousRecordsWithIndexes = (for {
        amls <- refNumber
      } yield records.zipWithIndex.filter(_._1.amlsRegistrationNumber != amls))
      Ok(view(
        businessName,
        toTable(currentRecordsWithIndexes, "current-application-notifications"),
        previousRecordsWithIndexes.flatMap(recordsWithIndex =>
          if(recordsWithIndex.nonEmpty) {
            Some(toTable(recordsWithIndex, "previous-application-notifications"))
          } else {
            None
          }
        )
      ))
    }) getOrElse (throw new Exception("Cannot retrieve business name"))
  }

  def messageDetails(id: String, contactType: ContactType, amlsRegNo: String, templateVersion: String) = authAction.async {
      implicit request =>
        statusService.getReadStatus(amlsRegNo, request.accountTypeId) flatMap {
          case readStatus if readStatus.safeId.isDefined =>
            (for {
              safeId <- OptionT.fromOption[Future](readStatus.safeId)
              businessName <- BusinessName.getName(request.credId, readStatus.safeId, request.accountTypeId)
              details <- OptionT(amlsNotificationService.getMessageDetails(amlsRegNo, id, contactType, templateVersion, request.accountTypeId))
              status <- OptionT.liftF(statusService.getStatus(Some(amlsRegNo), request.accountTypeId, request.credId))
            } yield contactTypeToResponse(contactType, (amlsRegNo, safeId), businessName, details, status, templateVersion)) getOrElse NotFound(notFoundView)
          case r if r.safeId.isEmpty => throw new Exception("Unable to retrieve SafeID")
          case _ => Future.successful(BadRequest)
        }
  }

  private def contactTypeToResponse(contactType: ContactType,
                                    reference: (String, String),
                                    businessName: String,
                                    details: NotificationDetails,
                                    status: SubmissionStatus,
                                    templateVersion: String)(implicit request: Request[_], m: Messages) = {

    val msgText = details.messageText.getOrElse("")

    val (amlsRefNo, safeId) = reference

    def getTemplate[T](templateVersion : String, templateName: String): T =
      notificationsMap(templateVersion, templateName).asInstanceOf[T]

    def render(templateName: String, notificationParams: NotificationParams, templateVersion: String) =
      getTemplate[Template5[NotificationParams, Request[_], Messages, Lang, ApplicationConfig, play.twirl.api.Html]](templateVersion, templateName)
        .render(notificationParams, request, m, lang, appConfig)

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
              msgTitle = details.subject(templateVersion), msgContent = msgText, safeId = safeId.some), templateVersion)
          }
          case _ =>
            render("message_details", NotificationParams(
              msgTitle = details.subject(templateVersion), msgContent = msgText), templateVersion)
        }
    }
    Ok(notification)
  }

  private def toTable(rowsWithIndex: Seq[(NotificationRow, Int)], id: String): Table = {
    Table(
      rowsWithIndex.map { case(row, index) =>
        row.asTableRows(id, index)
      }
    )
  }
}