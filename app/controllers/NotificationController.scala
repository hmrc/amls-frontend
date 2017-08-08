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

import cats.data.OptionT
import cats.implicits._
import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.{AmlsConnector, DataCacheConnector}
import models.businessmatching.BusinessMatching
import models.notifications.ContactType._
import models.notifications._
import play.api.Play
import play.api.mvc.Request
import services.{AuthEnrolmentsService, NotificationService, StatusService}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{BusinessName, FeatureToggle}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait NotificationController extends BaseController {

  protected[controllers] implicit val dataCacheConnector: DataCacheConnector

  protected[controllers] def authEnrolmentsService: AuthEnrolmentsService

  protected[controllers] def statusService: StatusService

  protected[controllers] lazy val amlsNotificationService: NotificationService = Play.current.injector.instanceOf[NotificationService]

  protected[controllers] implicit val amlsConnector: AmlsConnector

  def getMessages = FeatureToggle(ApplicationConfig.notificationsToggle) {
    Authorised.async {
      implicit authContext =>
        implicit request =>
          statusService.getReadStatus flatMap {
            case readStatus if readStatus.safeId.isDefined => {
              (for {
                businessName <- BusinessName.getName(readStatus.safeId)
                records <- OptionT.liftF(amlsNotificationService.getNotifications(readStatus.safeId.get))
              } yield {
                Ok(views.html.notifications.your_messages(businessName, records))
              }) getOrElse (throw new Exception("Cannot retrieve business name"))
            }
            case _ => throw new Exception("Unable to retrieve SafeID")
          }
    }
  }

  def messageDetails(id: String, contactType: ContactType, amlsRegNo: String) = FeatureToggle(ApplicationConfig.notificationsToggle) {
    Authorised.async {
      implicit authContext =>
        implicit request =>
          statusService.getReadStatus flatMap {
            case readStatus if readStatus.safeId.isDefined =>
              (for {
                businessName <- BusinessName.getName(readStatus.safeId)
                details <- OptionT(amlsNotificationService.getMessageDetails(amlsRegNo, id, contactType))
              } yield contactTypeToResponse(contactType, amlsRegNo, businessName, details)) getOrElse NotFound(notFoundView)
            case r if r.safeId.isEmpty => throw new Exception("Unable to retrieve SafeID")
            case _ => Future.successful(BadRequest)
          }
    }
  }

  private def contactTypeToResponse(contactType: ContactType, reference: String, businessName: String, details: NotificationDetails)
                                   (implicit request: Request[_]) = {
    val msgText = details.messageText.getOrElse("")

    contactType match {
      case MindedToRevoke => Ok(views.html.notifications.minded_to_revoke(msgText, reference, businessName))
      case MindedToReject => Ok(views.html.notifications.minded_to_reject(msgText, reference, businessName))
      case RejectionReasons => Ok(views.html.notifications.rejection_reasons(msgText, reference, businessName, details.dateReceived))
      case RevocationReasons => Ok(views.html.notifications.revocation_reasons(msgText, reference, businessName, details.dateReceived))
      case NoLongerMindedToReject => Ok(views.html.notifications.no_longer_minded_to_reject(msgText, reference))
      case NoLongerMindedToRevoke => Ok(views.html.notifications.no_longer_minded_to_revoke(msgText, reference))
      case _ => Ok(views.html.notifications.message_details(details.subject, msgText))
    }
  }
}

object NotificationController extends NotificationController {
  // $COVERAGE-OFF$
  override protected[controllers] val dataCacheConnector = DataCacheConnector
  override protected[controllers] val authEnrolmentsService = AuthEnrolmentsService
  override protected[controllers] val statusService = StatusService
  override protected val authConnector = AMLSAuthConnector
  override protected[controllers] lazy val amlsConnector = AmlsConnector
}
