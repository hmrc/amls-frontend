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

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.declaration.AddPersonFormProvider
import models.businessmatching.BusinessMatching
import models.declaration.AddPerson
import models.declaration.release7._
import models.status._
import play.api.data.Form
import play.api.mvc._
import services.StatusService
import utils.{AuthAction, ControllerHelper}
import views.html.declaration.AddPersonView

import scala.concurrent.Future

class AddPersonController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val statusService: StatusService,
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: AddPersonFormProvider,
  view: AddPersonView
) extends AmlsBaseController(ds, cc) {

  def get(): Action[AnyContent] = authAction.async { implicit request =>
    addPersonView(request.amlsRefNumber, request.accountTypeId, request.credId, Ok, formProvider())
  }

  def getWithAmendment: Action[AnyContent] = get() // TODO this can be removed unless there is a GTM need for it

  def post(): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors =>
          dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key) flatMap { bm =>
            addPersonView(request.amlsRefNumber, request.accountTypeId, request.credId, BadRequest, formWithErrors)
          },
        data =>
          dataCacheConnector.save[AddPerson](request.credId, AddPerson.key, data) flatMap { _ =>
            statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId) map {
              case _ if isResponsiblePerson(data) =>
                Redirect(routes.RegisterResponsiblePersonController.get())
              case SubmissionReadyForReview       => Redirect(routes.DeclarationController.getWithAmendment())
              case _                              => Redirect(routes.DeclarationController.get())
            }
          }
      )
  }

  private def isResponsiblePerson(data: AddPerson): Boolean = {
    val roleList = data.roleWithinBusiness.items
    roleList.contains(BeneficialShareholder) ||
    roleList.contains(Director) ||
    roleList.contains(Partner) ||
    roleList.contains(SoleProprietor) ||
    roleList.contains(DesignatedMember) ||
    roleList.contains(NominatedOfficer)
  }

  private def addPersonView(
    amlsRegistrationNo: Option[String],
    accountTypeId: (String, String),
    cacheId: String,
    status: Status,
    form: Form[AddPerson]
  )(implicit request: Request[AnyContent]): Future[Result] =
    dataCacheConnector.fetch[BusinessMatching](cacheId, BusinessMatching.key) flatMap { bm =>
      val businessType = ControllerHelper.getBusinessType(bm)

      statusService.getStatus(amlsRegistrationNo, accountTypeId, cacheId) map {
        case SubmissionReady                                       =>
          status(view("declaration.addperson.title", "submit.registration", businessType, form))
        case SubmissionReadyForReview | SubmissionDecisionApproved =>
          status(view("declaration.addperson.amendment.title", "submit.amendment.application", businessType, form))
        case RenewalSubmitted(_)                                   =>
          status(view("declaration.addperson.title", "submit.amendment.application", businessType, form))
        case ReadyForRenewal(_)                                    =>
          status(view("declaration.addperson.title", "submit.renewal.application", businessType, form))
        case _                                                     => throw new Exception("Incorrect status - Page not permitted for this status")

      }
    }
}
