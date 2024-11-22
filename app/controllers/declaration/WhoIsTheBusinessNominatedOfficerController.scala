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

package controllers.declaration

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.declaration.BusinessNominatedOfficerFormProvider
import models.declaration.BusinessNominatedOfficer
import models.responsiblepeople.ResponsiblePerson.flowFromDeclaration
import models.responsiblepeople.{NominatedOfficer, Positions, ResponsiblePerson}
import models.status._
import play.api.Logging
import play.api.data.Form
import play.api.mvc._
import services.{RenewalService, SectionsProvider, StatusService}
import utils.{AuthAction, DeclarationHelper}
import views.html.declaration.SelectBusinessNominatedOfficerView

import javax.inject.Inject
import scala.concurrent.Future

class WhoIsTheBusinessNominatedOfficerController @Inject()(val dataCacheConnector: DataCacheConnector,
                                                           authAction: AuthAction,
                                                           val ds: CommonPlayDependencies,
                                                           val statusService: StatusService,
                                                           val cc: MessagesControllerComponents,
                                                           formProvider: BusinessNominatedOfficerFormProvider,
                                                           val sectionsProvider: SectionsProvider,
                                                           renewalService: RenewalService,
                                                           view: SelectBusinessNominatedOfficerView) extends AmlsBaseController(ds, cc) with Logging {

  private def businessNominatedOfficerView(amlsRegistrationNo: Option[String],
                                           accountTypeId: (String, String),
                                           cacheId: String,
                                           status: Status,
                                           form: Form[BusinessNominatedOfficer],
                                           rp: Seq[ResponsiblePerson])
                                          (implicit request: Request[AnyContent]): Future[Result] =
    statusService.getStatus(amlsRegistrationNo, accountTypeId, cacheId) map {
      case SubmissionReady => status(view("submit.registration", form, rp))
      case SubmissionReadyForReview | SubmissionDecisionApproved => status(view("submit.amendment.application", form, rp))
      case ReadyForRenewal(_) |  RenewalSubmitted (_) => status(view("submit.renewal.application", form, rp))
      case _ => throw new Exception("Incorrect status - Page not permitted for this status")
    }

  def get: Action[AnyContent] = authAction.async { implicit request =>

    lazy val whenSectionsComplete = dataCacheConnector.fetchAll(request.credId) flatMap { optionalCache =>
      (for {
        cache <- optionalCache
        responsiblePeople <- cache.getEntry[Seq[ResponsiblePerson]](ResponsiblePerson.key)
      } yield businessNominatedOfficerView(request.amlsRefNumber, request.accountTypeId, request.credId, Ok, formProvider(), ResponsiblePerson.filter(responsiblePeople))
      ) getOrElse businessNominatedOfficerView(request.amlsRefNumber, request.accountTypeId, request.credId, Ok, formProvider(), Seq.empty)
    }

    for {
      isRenewal <- renewalService.isRenewalFlow(request.amlsRefNumber, request.accountTypeId, request.credId)
      sectionsComplete <- DeclarationHelper.sectionsComplete(request.credId, sectionsProvider, isRenewal)
      result <- sectionsComplete match {
        case true => whenSectionsComplete
        case false =>
          logger.warn("Sections aren't complete, redirecting")
          Future.successful(Redirect(controllers.routes.RegistrationProgressController.get().url))
      }
    } yield result
  }

  def getWithAmendment: Action[AnyContent] = get //TODO this can be removed unless there is a GTM need for it

  private def updateNominatedOfficer(eventualMaybePeoples: Option[Seq[ResponsiblePerson]],
                                     data: BusinessNominatedOfficer): Future[Option[Seq[ResponsiblePerson]]] = {
    eventualMaybePeoples match {
      case Some(rpSeq) =>
        val updatedList = ResponsiblePerson.filter(rpSeq).map { responsiblePerson =>
          if (responsiblePerson.personName.exists(name => name.fullNameWithoutSpace.equals(data.value))) {
            val position = responsiblePerson.positions.fold[Option[Positions]](None)(p => Some(Positions(p.positions.+(NominatedOfficer), p.startDate)))
            responsiblePerson.copy(positions = position)
          } else {
            responsiblePerson
          }
        }
        Future.successful(Some(updatedList))
      case _ => Future.successful(eventualMaybePeoples)
    }
  }

  def post: Action[AnyContent] = authAction.async {
    implicit request =>
      validateRequest(request.amlsRefNumber, request.accountTypeId, request.credId, formProvider()){ data =>
          data.value match {
            case "-1" => Future.successful(Redirect(controllers.responsiblepeople.routes.ResponsiblePeopleAddController.get(displayGuidance = true, Some(flowFromDeclaration))))
            case _ => for {
              serviceStatus <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
              responsiblePeople <- dataCacheConnector.fetch[Seq[ResponsiblePerson]](request.credId, ResponsiblePerson.key)
              rp <- updateNominatedOfficer(responsiblePeople, data)
              _ <- dataCacheConnector.save(request.credId, ResponsiblePerson.key, rp)
            } yield {
              Redirect(routes.WhoIsRegisteringController.get)
            }
          }
      }
  }

  private def validateRequest(amlsRegistrationNo: Option[String],
                              accountTypeId: (String, String),
                              cacheId: String,
                              form: Form[BusinessNominatedOfficer])
                             (fn: BusinessNominatedOfficer => Future[Result])
                             (implicit request: Request[AnyContent]): Future[Result] = {
    form.bindFromRequest().fold(
      formWithErrors => dataCacheConnector.fetch[Seq[ResponsiblePerson]](cacheId, ResponsiblePerson.key) flatMap {
        case Some(data) => businessNominatedOfficerView(amlsRegistrationNo, accountTypeId, cacheId, BadRequest, formWithErrors, ResponsiblePerson.filter(data))
        case None => businessNominatedOfficerView(amlsRegistrationNo, accountTypeId, cacheId, BadRequest, formWithErrors, Seq.empty)
      },
      data => fn(data)
    )
  }
}
