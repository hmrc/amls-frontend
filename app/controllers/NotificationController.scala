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

import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.{AmlsNotificationConnector, DataCacheConnector}
import models.businessmatching.BusinessMatching
import models.notifications._
import play.api.Play
import play.api.i18n.Messages
import services.{AuthEnrolmentsService, NotificationService}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.FeatureToggle

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait NotificationController extends BaseController {

  protected[controllers] val dataCacheConnector: DataCacheConnector

  protected[controllers] def authEnrolmentsService: AuthEnrolmentsService

  protected[controllers] lazy val amlsNotificationService: NotificationService = Play.current.injector.instanceOf[NotificationService]

  def getMessages() = FeatureToggle(ApplicationConfig.notificationsToggle) {
    Authorised.async {
      implicit authContext =>
        implicit request =>
          authEnrolmentsService.amlsRegistrationNumber flatMap {
            case Some(amlsRegNo) => {
              dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key) flatMap { businessMatching =>
                (for {
                  bm <- businessMatching
                  rd <- bm.reviewDetails
                } yield {
                  amlsNotificationService.getNotifications(amlsRegNo) map { records =>
                    Ok(views.html.notifications.your_messages(rd.businessName, records))
                  }
                }) getOrElse (throw new Exception("Cannot retrieve business name"))
              }
            }
            case _ => throw new Exception("amlsRegNo does not exist")
          }
    }
  }


  def messageDetails(id: String, contactType:ContactType) = FeatureToggle(ApplicationConfig.notificationsToggle) {
    Authorised.async {
      implicit authContext =>
        implicit request =>
          authEnrolmentsService.amlsRegistrationNumber flatMap {
            case Some(regNo) => {
              amlsNotificationService.getMessageDetails(regNo, id, contactType) map {
                case Some(msg) => Ok(views.html.notifications.message_details(msg.subject, msg.messageText.getOrElse(Messages(msg.subject))))
                case None => NotFound(notFoundView)
              }
            }
            case _ => Future.successful(BadRequest)
          }
    }
  }
}

object NotificationController extends NotificationController {
  // $COVERAGE-OFF$
  override protected[controllers] val dataCacheConnector = DataCacheConnector
  override protected[controllers] val authEnrolmentsService = AuthEnrolmentsService
  override protected val authConnector = AMLSAuthConnector
}
