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

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.BusinessMatching
import models.declaration.AddPerson
import models.declaration.release7._
import models.status.{ReadyForRenewal, SubmissionReadyForReview}
import play.api.mvc.{AnyContent, Request, Result}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.ControllerHelper

import scala.concurrent.Future

trait AddPersonController extends BaseController {

  val dataCacheConnector: DataCacheConnector
  val statusService: StatusService

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
          addPersonView(BadRequest,f)
        case ValidForm(_, data) =>
          dataCacheConnector.save[AddPerson](AddPerson.key, data) flatMap { _ =>
            statusService.getStatus map {
              case _ if isResponsiblePerson(data) => {
                Redirect(routes.RegisterResponsiblePersonController.get())
              }
              case SubmissionReadyForReview if AmendmentsToggle.feature => Redirect(routes.DeclarationController.getWithAmendment())
              case _ => Redirect(routes.DeclarationController.get())
            }
          }
      }
    }
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

  private def addPersonView(status: Status, form: Form2[AddPerson])
                           (implicit auth: AuthContext, request: Request[AnyContent]): Future[Result] = {

    dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key) flatMap { bm =>
      val businessType = ControllerHelper.getBusinessType(bm)
        statusService.getStatus map {
          case SubmissionReadyForReview if AmendmentsToggle.feature =>
            status(views.html.declaration.add_person("declaration.addperson.amendment.title", "submit.amendment.application", businessType, form))
          case ReadyForRenewal(_) => status(views.html.declaration.add_person("declaration.addperson.title", "submit.renewal.application", businessType, form))
          case _ => status(views.html.declaration.add_person("declaration.addperson.title", "submit.registration", businessType, form))
        }
      }
    }
}

object AddPersonController extends AddPersonController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val statusService: StatusService = StatusService
}
