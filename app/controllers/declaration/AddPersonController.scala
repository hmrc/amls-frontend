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

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.businessmatching.{BusinessMatching, BusinessType}
import models.declaration.AddPerson
import models.declaration.release7._
import models.status._
import play.api.mvc.{AnyContent, Request, Result}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper

import scala.concurrent.Future

class AddPersonController @Inject () (
                                       val dataCacheConnector: DataCacheConnector,
                                       val statusService: StatusService,
                                       val authConnector: AuthConnector
                                     ) extends BaseController {


  def get() = Authorised.async {
    implicit authContext => implicit request => {
          addPersonView(Ok,EmptyForm)
      }
  }

  def getWithAmendment() = get()

  def post() = Authorised.async {
    implicit authContext => implicit request => {
      Form2[AddPerson](request.body) match {
        case f: InvalidForm =>

          dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key) flatMap { bm =>
            val bt = ControllerHelper.getBusinessType(bm)
            val newForm = updateFormErrors(f, bt)
            addPersonView(BadRequest, newForm)
          }

        case ValidForm(_, data) =>
          dataCacheConnector.save[AddPerson](AddPerson.key, data) flatMap { _ =>
            statusService.getStatus map {
              case _ if isResponsiblePerson(data) => {
                Redirect(routes.RegisterResponsiblePersonController.get())
              }
              case SubmissionReadyForReview => Redirect(routes.DeclarationController.getWithAmendment())
              case _ => Redirect(routes.DeclarationController.get())
            }
          }
      }
    }
  }

  private def isResponsiblePerson(data: AddPerson): Boolean = { val roleList = data.roleWithinBusiness.items
    roleList.contains(BeneficialShareholder) ||
    roleList.contains(Director) ||
    roleList.contains(Partner) ||
    roleList.contains(SoleProprietor) ||
    roleList.contains(DesignatedMember) ||
    roleList.contains(NominatedOfficer)
  }

  private def updateFormErrors(f: InvalidForm, businessType: Option[BusinessType]) = {
    val message = businessType match {
      case Some(BusinessType.LimitedCompany) => "Select if you are a beneficial shareholder, an external accountant, a director, a nominated officer, or other"
      case Some(BusinessType.SoleProprietor) => "Select if you are an external accountant, a nominated officer, a sole proprietor or other"
      case _ => "not implemented yet"
    }

    val newErrors: Seq[(Path, Seq[ValidationError])] = f.errors.map {
      case (p, _) if p == Path("positions") => (Path("positions"), Seq(ValidationError(Seq(message))))
      case (p, s) => (p, s)
    }

    f.copy(errors = newErrors)
  }

  private def addPersonView(status: Status, form: Form2[AddPerson])
                           (implicit auth: AuthContext, request: Request[AnyContent]): Future[Result] = {

    dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key) flatMap { bm =>
      val businessType = ControllerHelper.getBusinessType(bm)
      val formWithModifiedErrors = form

        statusService.getStatus map {
          case SubmissionReady =>
            status(views.html.declaration.add_person("declaration.addperson.title", "submit.registration", businessType, formWithModifiedErrors))
          case SubmissionReadyForReview | SubmissionDecisionApproved =>
            status(views.html.declaration.add_person("declaration.addperson.amendment.title", "submit.amendment.application", businessType, formWithModifiedErrors))
          case RenewalSubmitted(_) => status(views.html.declaration.add_person("declaration.addperson.title", "submit.amendment.application", businessType, formWithModifiedErrors))
          case ReadyForRenewal(_) => status(views.html.declaration.add_person("declaration.addperson.title", "submit.renewal.application", businessType, formWithModifiedErrors))
          case _ => throw new Exception("Incorrect status - Page not permitted for this status")

        }
      }
    }
}
