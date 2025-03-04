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

package controllers

import cats.data.OptionT
import cats.implicits._
import connectors.{AmlsConnector, DataCacheConnector}
import models.notifications._
import play.api.mvc.{MessagesControllerComponents, Request, Result}
import services.businessmatching.BusinessMatchingService
import services.{AuthEnrolmentsService, NotificationService, StatusService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, BusinessName, NotificationTemplateGenerator}
import views.html.notifications.YourMessagesView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class NotificationController @Inject() (
  val authEnrolmentsService: AuthEnrolmentsService,
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
  notificationTemplateGenerator: NotificationTemplateGenerator
) extends AmlsBaseController(ds, cc) {

  def getMessages = authAction.async { implicit request =>
    request.amlsRefNumber match {
      case Some(mlrRegNumber) =>
        statusService.getReadStatus(mlrRegNumber, request.accountTypeId) flatMap {
          case readStatus if readStatus.safeId.isDefined =>
            generateNotificationView(request.credId, readStatus.safeId.get, Some(mlrRegNumber), request.accountTypeId)
          case _                                         => throw new Exception("Unable to retrieve SafeID")
        }
      case _                  =>
        businessMatchingService.getModel(request.credId).value flatMap {
          case Some(model) if model.reviewDetails.isDefined =>
            generateNotificationView(request.credId, model.reviewDetails.get.safeId, None, request.accountTypeId)
          case _                                            => throw new Exception("Unable to retrieve SafeID from reviewDetails")
        }
    }
  }

  def messageDetails(id: String, contactType: ContactType, amlsRegNo: String, templateVersion: String) =
    authAction.async { implicit request =>
      statusService.getReadStatus(amlsRegNo, request.accountTypeId) flatMap {
        case readStatus if readStatus.safeId.isDefined =>
          (for {
            safeId       <- OptionT.fromOption[Future](readStatus.safeId)
            businessName <- BusinessName.getName(request.credId, readStatus.safeId, request.accountTypeId)
            details      <- OptionT(
                              amlsNotificationService
                                .getMessageDetails(amlsRegNo, id, contactType, templateVersion, request.accountTypeId)
                            )
            status       <- OptionT.liftF(statusService.getStatus(Some(amlsRegNo), request.accountTypeId, request.credId))
          } yield Ok(
            notificationTemplateGenerator
              .contactTypeToView(contactType, (amlsRegNo, safeId), businessName, details, status, templateVersion)
          )) getOrElse NotFound(notFoundView)
        case r if r.safeId.isEmpty                     => throw new Exception("Unable to retrieve SafeID")
        case _                                         => Future.successful(BadRequest)
      }
    }

  private def generateNotificationView(
    credId: String,
    safeId: String,
    refNumber: Option[String],
    accountTypeId: (String, String)
  )(implicit hc: HeaderCarrier, request: Request[_]): Future[Result] =
    (for {
      businessName                  <- BusinessName.getName(credId, Some(safeId), accountTypeId)
      records: Seq[NotificationRow] <- OptionT.liftF(amlsNotificationService.getNotifications(safeId, accountTypeId))
    } yield {
      val currentRecordsWithIndexes  = (for {
        amls <- refNumber
      } yield records.zipWithIndex.filter(_._1.amlsRegistrationNumber == amls)) getOrElse records.zipWithIndex
      val previousRecordsWithIndexes = for {
        amls <- refNumber
      } yield records.zipWithIndex.filter(_._1.amlsRegistrationNumber != amls)
      Ok(
        view(
          businessName,
          notificationTemplateGenerator
            .toTable(currentRecordsWithIndexes, "current-application-notifications", isPrevRegistration = false),
          previousRecordsWithIndexes.flatMap(recordsWithIndex =>
            if (recordsWithIndex.nonEmpty) {
              Some(
                notificationTemplateGenerator
                  .toTable(recordsWithIndex, "previous-application-notifications", isPrevRegistration = true)
              )
            } else {
              None
            }
          )
        )
      )
    }) getOrElse (throw new Exception("Cannot retrieve business name"))

}
