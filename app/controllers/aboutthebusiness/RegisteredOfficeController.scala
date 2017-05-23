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

package controllers.aboutthebusiness

import audit.{AddressCreatedEvent, AddressModifiedEvent}
import config.{AMLSAuditConnector, AMLSAuthConnector}
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.aboutthebusiness.{AboutTheBusiness, RegisteredOffice, RegisteredOfficeUK}
import models.status.{ReadyForRenewal, SubmissionDecisionApproved}
import play.api.mvc.{Request, Result}
import services.StatusService
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import utils.DateOfChangeHelper
import views.html.aboutthebusiness._
import audit.AddressConversions._
import cats.data.OptionT
import cats.implicits._
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{Future, Promise}

trait RegisteredOfficeController extends BaseController with DateOfChangeHelper {

  val dataCacheConnector: DataCacheConnector
  val statusService: StatusService
  val auditConnector: AuditConnector

  private val preSelectUK = RegisteredOfficeUK("", "", None, None, "")

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        dataCacheConnector.fetch[AboutTheBusiness](AboutTheBusiness.key) map {
          response =>
            val form: Form2[RegisteredOffice] = (for {
              aboutTheBusiness <- response
              registeredOffice <- aboutTheBusiness.registeredOffice
            } yield Form2[RegisteredOffice](registeredOffice)).getOrElse(Form2[RegisteredOffice](preSelectUK))
            Ok(registered_office(form, edit))

        }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
        Form2[RegisteredOffice](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(registered_office(f, edit)))
          case ValidForm(_, data) =>

            val doUpdate = for {
              aboutTheBusiness <- OptionT(dataCacheConnector.fetch[AboutTheBusiness](AboutTheBusiness.key))
              _ <- OptionT.liftF(dataCacheConnector.save[AboutTheBusiness](AboutTheBusiness.key, aboutTheBusiness.registeredOffice(data)))
              status <- OptionT.liftF(statusService.getStatus)
              _ <- OptionT.liftF(auditAddressChange(data, aboutTheBusiness.registeredOffice, edit)) orElse OptionT.some(Success)
            } yield {
              if (redirectToDateOfChange[RegisteredOffice](status, aboutTheBusiness.registeredOffice, data)) {
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

object RegisteredOfficeController extends RegisteredOfficeController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val statusService = StatusService
  override lazy val auditConnector = AMLSAuditConnector
}
