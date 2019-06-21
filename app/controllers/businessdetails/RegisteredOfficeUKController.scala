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
import cats.data.OptionT
import cats.implicits._
import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.businessdetails.{BusinessDetails, RegisteredOffice, RegisteredOfficeUK}
import play.api.mvc.Request
import services.{AutoCompleteService, StatusService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.DateOfChangeHelper
import views.html.businessdetails._

import scala.concurrent.Future

class RegisteredOfficeUKController @Inject ()(
                                            val dataCacheConnector: DataCacheConnector,
                                            val statusService: StatusService,
                                            val auditConnector: AuditConnector,
                                            val authConnector: AuthConnector
                                            ) extends BaseController with DateOfChangeHelper {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        dataCacheConnector.fetch[BusinessDetails](BusinessDetails.key) map {
          response =>
            val form: Form2[RegisteredOffice] = (for {
              businessDetails <- response
              registeredOffice <- businessDetails.registeredOffice
            } yield Form2[RegisteredOffice](registeredOffice)) getOrElse EmptyForm
            Ok(registered_office_uk(form, edit))

        }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
        Form2[RegisteredOffice](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(registered_office_uk(f, edit)))
          case ValidForm(_, data) =>

            val doUpdate = for {
              businessDetails <- OptionT(dataCacheConnector.fetch[BusinessDetails](BusinessDetails.key))
              _ <- OptionT.liftF(dataCacheConnector.save[BusinessDetails](BusinessDetails.key, businessDetails.registeredOffice(data)))
              status <- OptionT.liftF(statusService.getStatus)
              _ <- OptionT.liftF(auditAddressChange(data, businessDetails.registeredOffice, edit)) orElse OptionT.some(Success)
            } yield {
              if (redirectToDateOfChange[RegisteredOffice](status, businessDetails.registeredOffice, data)) {
                Redirect(routes.RegisteredOfficeDateOfChangeController.get())
              } else {
                edit match {
                  case true => Redirect(routes.SummaryController.get())
                  case _ => Redirect(routes.ContactingYouController.get(edit))
                }
              }
            }

            doUpdate getOrElse InternalServerError("Unable to update registered office")
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