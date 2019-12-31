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

package controllers.businessdetails

import audit.AddressConversions._
import audit.{AddressCreatedEvent, AddressModifiedEvent}
import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms._
import models.businessdetails.{BusinessDetails, RegisteredOffice}
import play.api.mvc.{MessagesControllerComponents, Request}
import services.{AutoCompleteService, StatusService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import utils.{AuthAction, DateOfChangeHelper}
import views.html.businessdetails._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class RegisteredOfficeNonUKController @Inject ()(
                                                  val dataCacheConnector: DataCacheConnector,
                                                  val statusService: StatusService,
                                                  val auditConnector: AuditConnector,
                                                  val autoCompleteService: AutoCompleteService,
                                                  val authAction: AuthAction,
                                                  val ds: CommonPlayDependencies,
                                                  val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) with DateOfChangeHelper {

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        dataCacheConnector.fetch[BusinessDetails](request.credId, BusinessDetails.key) map {
          response =>
            val form: Form2[RegisteredOffice] = (for {
              businessDetails <- response
              registeredOffice <- businessDetails.registeredOffice
            } yield Form2[RegisteredOffice](registeredOffice)) getOrElse EmptyForm
            Ok(registered_office_non_uk(form, edit, autoCompleteService.getCountries))
        }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request =>
        Form2[RegisteredOffice](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(registered_office_non_uk(f, edit, autoCompleteService.getCountries)))
          case ValidForm(_, data) =>
            for {
              businessDetails <- dataCacheConnector.fetch[BusinessDetails](request.credId, BusinessDetails.key)
              _ <- dataCacheConnector.save[BusinessDetails](request.credId, BusinessDetails.key, businessDetails.registeredOffice(data))
              status <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
              _ <- auditAddressChange(data, businessDetails flatMap { _.registeredOffice }, edit)
            } yield {
              if (redirectToDateOfChange[RegisteredOffice](status, businessDetails.registeredOffice, data)) {
                Redirect(routes.RegisteredOfficeDateOfChangeController.get())
              } else {
                edit match {
                  case true => Redirect(routes.SummaryController.get())
                  case _ => Redirect(routes.ContactingYouController.get())
                }
              }
            }
        }
  }

  def auditAddressChange(currentAddress: RegisteredOffice, oldAddress: Option[RegisteredOffice], edit: Boolean)
                        (implicit hc: HeaderCarrier, request: Request[_]): Future[AuditResult] = {
    if (edit) {
      auditConnector.sendEvent(AddressModifiedEvent(currentAddress, oldAddress))
    } else {
      auditConnector.sendEvent(AddressCreatedEvent(currentAddress))
    }
  }
}
