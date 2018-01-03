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

package controllers.declaration

import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.DataCacheConnector
import controllers.BaseController
import models.declaration.AddPerson
import models.status.{ReadyForRenewal, SubmissionReadyForReview}
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, Result}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

trait DeclarationController extends BaseController {

  def dataCacheConnector: DataCacheConnector
  def statusService: StatusService

  lazy val defaultView = declarationView("declaration.declaration.title", "submit.registration", isAmendment = false)

  def get() = Authorised.async {
    implicit authContext => implicit request => {
      dataCacheConnector.fetch[AddPerson](AddPerson.key) flatMap {
        case Some(addPerson) => {
          val name = s"${addPerson.firstName} ${addPerson.middleName getOrElse ""} ${addPerson.lastName}"
          for{
            status <- statusService.getStatus
          } yield status match {
            case ReadyForRenewal(_) => Ok(
              views.html.declaration.declare("declaration.declaration.amendment.title", "submit.renewal.application", name, false))
            case SubmissionReadyForReview if AmendmentsToggle.feature => Ok(
              views.html.declaration.declare("declaration.declaration.amendment.title", "submit.amendment.application", name, true))
            case _ => Ok(
              views.html.declaration.declare("declaration.declaration.amendment.title", "submit.registration", name, false))
          }
        }
        case _ => redirectToAddPersonPage
      }
    }
  }

  def getWithAmendment = AmendmentsToggle.feature match {
    case b@true => declarationView("declaration.declaration.amendment.title", "submit.amendment.application", b)
    case _ => defaultView
  }

  private def declarationView(title: String, subtitle: String, isAmendment: Boolean) = Authorised.async {
    implicit authcontext => implicit request =>
      dataCacheConnector.fetch[AddPerson](AddPerson.key) flatMap {
        case Some(addPerson) =>
          val name = s"${addPerson.firstName} ${addPerson.middleName getOrElse ""} ${addPerson.lastName}"
          Future.successful(Ok(views.html.declaration.declare(title, subtitle, name, isAmendment)))
        case _ => redirectToAddPersonPage
      }
  }

  private def redirectToAddPersonPage(implicit hc: HeaderCarrier, auth: AuthContext): Future[Result] =
    statusService.getStatus map {
      case SubmissionReadyForReview if AmendmentsToggle.feature => Redirect(routes.AddPersonController.getWithAmendment())
      case _ => Redirect(routes.AddPersonController.get())
    }

}

object DeclarationController extends DeclarationController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val statusService = StatusService
}
