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

package controllers.declaration

import javax.inject.Inject

import config.AppConfig
import connectors.{AmlsConnector, DataCacheConnector}
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.declaration.BusinessNominatedOfficer
import models.responsiblepeople.ResponsiblePeople.flowFromDeclaration
import models.responsiblepeople.{NominatedOfficer, Positions, ResponsiblePeople}
import models.status._
import play.api.mvc.{AnyContent, Request, Result}
import services.StatusService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.StatusConstants
import views.html.declaration.select_business_nominated_officer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WhoIsTheBusinessNominatedOfficerController @Inject ()(
                                                             val amlsConnector: AmlsConnector,
                                                             val dataCacheConnector: DataCacheConnector,
                                                             val authConnector: AuthConnector,
                                                             val statusService: StatusService,
                                                             config: AppConfig) extends BaseController {

  def businessNominatedOfficerView(status: Status, form: Form2[BusinessNominatedOfficer], rp: Seq[ResponsiblePeople])
                                  (implicit auth: AuthContext, request: Request[AnyContent]): Future[Result] = {
    statusService.getStatus map {
      case SubmissionReady => status(select_business_nominated_officer("submit.registration", form, rp))
      case SubmissionReadyForReview | SubmissionDecisionApproved => status(select_business_nominated_officer("submit.amendment.application", form, rp))
      case ReadyForRenewal(_) |  RenewalSubmitted (_) => status(select_business_nominated_officer("submit.renewal.application", form, rp))
      case _ => throw new Exception("Incorrect status - Page not permitted for this status")
    }
  }

  def get = Authorised.async {
    implicit authContext =>
      implicit request =>
        dataCacheConnector.fetchAll flatMap {
          optionalCache =>
            (for {
              cache <- optionalCache
              responsiblePeople <- cache.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key)
            } yield businessNominatedOfficerView(Ok, EmptyForm, responsiblePeople.filter(!_.status.contains(StatusConstants.Deleted)))
              ) getOrElse businessNominatedOfficerView(Ok, EmptyForm, Seq.empty)
        }
  }

  def getWithAmendment() = get

  def updateNominatedOfficer(eventualMaybePeoples: Option[Seq[ResponsiblePeople]],
                             data: BusinessNominatedOfficer): Future[Option[Seq[ResponsiblePeople]]] = {
    eventualMaybePeoples match {
      case Some(rpSeq) =>
        val updatedList = ResponsiblePeople.filter(rpSeq).map { responsiblePerson =>
          responsiblePerson.personName.exists(name => name.fullNameWithoutSpace.equals(data.value)) match {
            case true =>
              val position = responsiblePerson.positions.fold[Option[Positions]](None)(p => Some(Positions(p.positions. + (NominatedOfficer), p.startDate)))
              responsiblePerson.copy(positions = position)
            case false => responsiblePerson
          }
        }
        Future.successful(Some(updatedList))
      case _ => Future.successful(eventualMaybePeoples)
    }
  }

  def post = Authorised.async {
    implicit authContext => implicit request =>
      validateRequest(Form2[BusinessNominatedOfficer](request.body)){ data =>
          data.value match {
            case "-1" => Future.successful(Redirect(controllers.responsiblepeople.routes.ResponsiblePeopleAddController.get(true, Some(flowFromDeclaration))))
            case _ => for {
              serviceStatus <- statusService.getStatus
              responsiblePeople <- dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key)
              rp <- updateNominatedOfficer(responsiblePeople, data)
              _ <- dataCacheConnector.save(ResponsiblePeople.key, rp)
            } yield serviceStatus match {
              case SubmissionReady | NotCompleted if config.showFeesToggle => Redirect(controllers.routes.FeeGuidanceController.get())
              case _ => Redirect(routes.WhoIsRegisteringController.get())
            }
          }
      }
  }

  def validateRequest(form: Form2[BusinessNominatedOfficer])
                     (fn: BusinessNominatedOfficer => Future[Result])
                     (implicit ac: AuthContext, hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] = {
    form match {
      case f: InvalidForm => dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key) flatMap {
        case Some(data) => businessNominatedOfficerView(BadRequest, f, data.filter(!_.status.contains(StatusConstants.Deleted)))
        case None => businessNominatedOfficerView(BadRequest, f, Seq.empty)
      }
      case ValidForm(_, data) => fn(data)
    }
  }
}