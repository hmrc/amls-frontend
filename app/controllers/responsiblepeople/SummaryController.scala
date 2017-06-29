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

package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.{BaseController, declaration}
import models.responsiblepeople.ResponsiblePeople
import models.status.{NotCompleted, SubmissionReady, SubmissionReadyForReview}
import services.StatusService
import models.status.{NotCompleted, SubmissionReady, SubmissionReadyForReview}
import services.StatusService
import utils.ControllerHelper
import views.html.responsiblepeople._

import scala.concurrent.Future

trait SummaryController extends BaseController {

  val dataCacheConnector: DataCacheConnector
  val statusService : StatusService

  def get(fromDeclaration: Option[String] = None) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key) map {
        case Some(data) =>
          val hasNonUKResident = ControllerHelper.hasNonUkResident(Some(data))
          Ok(check_your_answers(data, fromDeclaration, hasNonUKResident))
        case _ => Redirect(controllers.routes.RegistrationProgressController.get())
      }
    }

  def post(fromDeclaration: Option[String] = None) = Authorised.async {
    implicit authContext => implicit request =>
      fromDeclaration match {
        case None => Future.successful(Redirect(controllers.routes.RegistrationProgressController.get()))
        case Some("fromDeclaration") => {
          for {
            status <- statusService.getStatus
            hasNominatedOfficer <- ControllerHelper.hasNominatedOfficer(dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key))
          } yield status match {
            case SubmissionReady | NotCompleted => {
              hasNominatedOfficer match {
                case true => Redirect(controllers.routes.FeeGuidanceController.get())
                case false => Redirect(controllers.declaration.routes.WhoIsTheBusinessNominatedOfficerController.get())
              }
            }
            case SubmissionReadyForReview => {
              hasNominatedOfficer match {
                case true => Redirect(controllers.declaration.routes.WhoIsRegisteringController.get())
                case false => Redirect(controllers.declaration.routes.WhoIsTheBusinessNominatedOfficerController.get())
              }
            }
            case _ => {
              hasNominatedOfficer match {
                case true => Redirect(controllers.declaration.routes.WhoIsRegisteringController.getWithAmendment())
                case false => Redirect(controllers.declaration.routes.WhoIsTheBusinessNominatedOfficerController.getWithAmendment())
              }
            }
          }
        }
      }
    }
}

object SummaryController extends SummaryController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val statusService = StatusService
}
