/*
 * Copyright 2019 HM Revenue & Customs
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

import config.AppConfig
import connectors.{AmlsConnector, DataCacheConnector}
import controllers.DefaultBaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.declaration.BusinessNominatedOfficer
import models.responsiblepeople.ResponsiblePerson.flowFromDeclaration
import models.responsiblepeople.{NominatedOfficer, Positions, ResponsiblePerson}
import models.status._
import play.api.mvc.{AnyContent, Request, Result}
import services.StatusService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.AuthAction
import views.html.declaration.select_business_nominated_officer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WhoIsTheBusinessNominatedOfficerController @Inject ()(
                                                             val amlsConnector: AmlsConnector,
                                                             val dataCacheConnector: DataCacheConnector,
                                                             authAction: AuthAction,
                                                             val statusService: StatusService,
                                                             config: AppConfig) extends DefaultBaseController {
@deprecated("To be removed when new auth is implemented")
  def businessNominatedOfficerView(status: Status, form: Form2[BusinessNominatedOfficer], rp: Seq[ResponsiblePerson])
                                  (implicit auth: AuthContext, request: Request[AnyContent]): Future[Result] =
    statusService.getStatus map {
      case SubmissionReady => status(select_business_nominated_officer("submit.registration", form, rp))
      case SubmissionReadyForReview | SubmissionDecisionApproved => status(select_business_nominated_officer("submit.amendment.application", form, rp))
      case ReadyForRenewal(_) |  RenewalSubmitted (_) => status(select_business_nominated_officer("submit.renewal.application", form, rp))
      case _ => throw new Exception("Incorrect status - Page not permitted for this status")
    }

  def businessNominatedOfficerView(amlsRegistrationNo: Option[String],
                                   accountTypeId: (String, String),
                                   cacheId: String,
                                   status: Status,
                                   form: Form2[BusinessNominatedOfficer],
                                   rp: Seq[ResponsiblePerson])
                                  (implicit request: Request[AnyContent]): Future[Result] =
    statusService.getStatus(amlsRegistrationNo, accountTypeId, cacheId) map {
      case SubmissionReady => status(select_business_nominated_officer("submit.registration", form, rp))
      case SubmissionReadyForReview | SubmissionDecisionApproved => status(select_business_nominated_officer("submit.amendment.application", form, rp))
      case ReadyForRenewal(_) |  RenewalSubmitted (_) => status(select_business_nominated_officer("submit.renewal.application", form, rp))
      case _ => throw new Exception("Incorrect status - Page not permitted for this status")
    }

  def get = authAction.async {
      implicit request =>
        dataCacheConnector.fetchAll(request.credId) flatMap {
          optionalCache =>
            (for {
              cache <- optionalCache
              responsiblePeople <- cache.getEntry[Seq[ResponsiblePerson]](ResponsiblePerson.key)
            } yield businessNominatedOfficerView(request.amlsRefNumber, request.accountTypeId, request.credId, Ok, EmptyForm, ResponsiblePerson.filter(responsiblePeople))
              ) getOrElse businessNominatedOfficerView(request.amlsRefNumber, request.accountTypeId, request.credId, Ok, EmptyForm, Seq.empty)
        }
  }

  def getWithAmendment() = get

  def updateNominatedOfficer(eventualMaybePeoples: Option[Seq[ResponsiblePerson]],
                             data: BusinessNominatedOfficer): Future[Option[Seq[ResponsiblePerson]]] = {
    eventualMaybePeoples match {
      case Some(rpSeq) =>
        val updatedList = ResponsiblePerson.filter(rpSeq).map { responsiblePerson =>
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

  def post = authAction.async {
    implicit request =>
      validateRequest(request.amlsRefNumber, request.accountTypeId, request.credId, Form2[BusinessNominatedOfficer](request.body)){ data =>
          data.value match {
            case "-1" => Future.successful(Redirect(controllers.responsiblepeople.routes.ResponsiblePeopleAddController.get(true, Some(flowFromDeclaration))))
            case _ => for {
              serviceStatus <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
              responsiblePeople <- dataCacheConnector.fetch[Seq[ResponsiblePerson]](request.credId, ResponsiblePerson.key)
              rp <- updateNominatedOfficer(responsiblePeople, data)
              _ <- dataCacheConnector.save(request.credId, ResponsiblePerson.key, rp)
            } yield {
              Redirect(routes.WhoIsRegisteringController.get())
            }
          }
      }
  }

@deprecated("Remove once new auth upgrade is implemented")
  def validateRequest(form: Form2[BusinessNominatedOfficer])
                     (fn: BusinessNominatedOfficer => Future[Result])
                     (implicit ac: AuthContext, hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] = {
    form match {
      case f: InvalidForm => dataCacheConnector.fetch[Seq[ResponsiblePerson]](ResponsiblePerson.key) flatMap {
        case Some(data) => businessNominatedOfficerView(BadRequest, f, ResponsiblePerson.filter(data))
        case None => businessNominatedOfficerView(BadRequest, f, Seq.empty)
      }
      case ValidForm(_, data) => fn(data)
    }
  }

  def validateRequest(amlsRegistrationNo: Option[String],
                      accountTypeId: (String, String),
                      cacheId: String,
                      form: Form2[BusinessNominatedOfficer])
                     (fn: BusinessNominatedOfficer => Future[Result])
                     (implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] = {
    form match {
      case f: InvalidForm => dataCacheConnector.fetch[Seq[ResponsiblePerson]](cacheId, ResponsiblePerson.key) flatMap {
        case Some(data) => businessNominatedOfficerView(amlsRegistrationNo, accountTypeId, cacheId, BadRequest, f, ResponsiblePerson.filter(data))
        case None => businessNominatedOfficerView(amlsRegistrationNo, accountTypeId, cacheId, BadRequest, f, Seq.empty)
      }
      case ValidForm(_, data) => fn(data)
    }
  }
}