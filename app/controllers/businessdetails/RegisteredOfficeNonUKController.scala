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

package controllers.businessdetails

import audit.AddressConversions._
import audit.{AddressCreatedEvent, AddressModifiedEvent}
import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.businessdetails.RegisteredOfficeNonUkFormProvider
import models.businessdetails.{BusinessDetails, RegisteredOffice}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services.{AutoCompleteService, StatusService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import utils.{AuthAction, DateOfChangeHelper}
import views.html.businessdetails.RegisteredOfficeNonUKView

import scala.concurrent.Future

class RegisteredOfficeNonUKController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val statusService: StatusService,
  val auditConnector: AuditConnector,
  val autoCompleteService: AutoCompleteService,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: RegisteredOfficeNonUkFormProvider,
  view: RegisteredOfficeNonUKView
) extends AmlsBaseController(ds, cc)
    with DateOfChangeHelper {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[BusinessDetails](request.credId, BusinessDetails.key) map { businessDetails =>
      Ok(
        view(
          businessDetails.registeredOffice.fold(formProvider())(formProvider().fill),
          edit,
          autoCompleteService.formOptions
        )
      )
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit, autoCompleteService.formOptions))),
        data =>
          for {
            businessDetails <- dataCacheConnector.fetch[BusinessDetails](request.credId, BusinessDetails.key)
            _               <- dataCacheConnector
                                 .save[BusinessDetails](request.credId, BusinessDetails.key, businessDetails.registeredOffice(data))
            status          <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
            _               <- auditAddressChange(data, businessDetails flatMap { _.registeredOffice }, edit)
          } yield
            if (redirectToDateOfChange[RegisteredOffice](status, businessDetails.registeredOffice, data)) {
              Redirect(routes.RegisteredOfficeDateOfChangeController.get)
            } else {
              if (edit) {
                Redirect(routes.SummaryController.get)
              } else {
                Redirect(routes.BusinessEmailAddressController.get())
              }
            }
      )
  }

  def auditAddressChange(currentAddress: RegisteredOffice, oldAddress: Option[RegisteredOffice], edit: Boolean)(implicit
    hc: HeaderCarrier,
    request: Request[_]
  ): Future[AuditResult] =
    if (edit) {
      auditConnector.sendEvent(AddressModifiedEvent(currentAddress, oldAddress))
    } else {
      auditConnector.sendEvent(AddressCreatedEvent(currentAddress))
    }
}
