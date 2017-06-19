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
import connectors.DataCacheConnector
import models.businessmatching.BusinessMatching
import models.notifications.ContactType._
import models.notifications._
import play.api.Play
import services.{AuthEnrolmentsService, NotificationService, StatusService}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.FeatureToggle

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait NotificationController extends BaseController {

  protected[controllers] val dataCacheConnector: DataCacheConnector

  protected[controllers] def authEnrolmentsService: AuthEnrolmentsService

  protected[controllers] def statusService: StatusService

  protected[controllers] lazy val amlsNotificationService: NotificationService = Play.current.injector.instanceOf[NotificationService]

  def getMessages() = FeatureToggle(ApplicationConfig.notificationsToggle) {
    Authorised.async {
      implicit authContext =>
        implicit request =>
          statusService.getReadStatus flatMap {
            case readStatus if readStatus.safeId.isDefined => {
              (for {
                businessName <- OptionT(getBusinessName)
                records <- OptionT.liftF(amlsNotificationService.getNotifications(readStatus.safeId.get))
              } yield {
                Ok(views.html.notifications.your_messages(businessName, records))
              }) getOrElse (throw new Exception("Cannot retrieve business name"))
            }
            case _ => throw new Exception("Unable to retrieve SafeID")
          }
    }
  }

  def messageDetails(id: String, contactType: ContactType) = FeatureToggle(ApplicationConfig.notificationsToggle) {
    Authorised.async {
      implicit authContext =>
        implicit request =>
          statusService.getReadStatus flatMap {
            case readStatus if readStatus.safeId.isDefined => {
              (for {
                amlsRegNo <- OptionT(authEnrolmentsService.amlsRegistrationNumber)
                businessName <- OptionT(getBusinessName)
                msg <- OptionT(amlsNotificationService.getMessageDetails(readStatus.safeId.get, id, contactType))
                msgText <- OptionT.fromOption[Future](msg.messageText)
              } yield {
                contactType match {
                  case MindedToRevoke => Ok(views.html.notifications.minded_to_revoke(msgText, amlsRegNo, businessName))
                  case MindedToReject => Ok(views.html.notifications.minded_to_reject(msgText, businessName))
                  case RejectionReasons => Ok(views.html.notifications.rejection_reasons(msgText, amlsRegNo, businessName, msg.dateReceived))
                  case RevocationReasons => Ok(views.html.notifications.revocation_reasons(msgText, amlsRegNo, businessName, msg.dateReceived))
                  case NoLongerMindedToReject => Ok(views.html.notifications.no_longer_minded_to_reject(msgText))
                  case NoLongerMindedToRevoke => Ok(views.html.notifications.no_longer_minded_to_revoke(msgText, amlsRegNo))
                  case _ => Ok(views.html.notifications.message_details(msg.subject, msgText))
                }
              }) getOrElse NotFound(notFoundView)
            }
            case r if r.safeId.isEmpty => throw new Exception("Unable to retrieve SafeID")
            case _ => Future.successful(BadRequest)
          }
    }
  }

  private def getBusinessName(implicit hc: HeaderCarrier, authContext: AuthContext): Future[Option[String]] = {
    dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key) map { businessMatching =>
      for {
        bm <- businessMatching
        rd <- bm.reviewDetails
      } yield rd.businessName
    }
  }

}

object NotificationController extends NotificationController {
  // $COVERAGE-OFF$
  override protected[controllers] val dataCacheConnector = DataCacheConnector
  override protected[controllers] val authEnrolmentsService = AuthEnrolmentsService
  override protected[controllers] val statusService = StatusService
  override protected val authConnector = AMLSAuthConnector
}
