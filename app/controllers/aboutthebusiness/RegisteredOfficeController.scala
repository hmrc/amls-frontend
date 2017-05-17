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

import audit.AddressCreatedEvent
import config.{AMLSAuditConnector, AMLSAuthConnector}
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.aboutthebusiness.{AboutTheBusiness, RegisteredOffice, RegisteredOfficeUK}
import models.status.{ReadyForRenewal, SubmissionDecisionApproved}
import play.api.mvc.Result
import services.StatusService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.DateOfChangeHelper
import views.html.aboutthebusiness._
import audit.AddressConversions._

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
    implicit authContext =>
      implicit request =>
        val resultPromise = Promise[Result]

        Form2[RegisteredOffice](request.body) match {
          case f: InvalidForm =>
            resultPromise success BadRequest(registered_office(f, edit))
          case ValidForm(_, data) =>
            for {
              aboutTheBusiness <-
              dataCacheConnector.fetch[AboutTheBusiness](AboutTheBusiness.key)
              _ <- dataCacheConnector.save[AboutTheBusiness](AboutTheBusiness.key,
                aboutTheBusiness.registeredOffice(data))
              status <- statusService.getStatus
            } yield {
              status match {
                case SubmissionDecisionApproved | ReadyForRenewal(_) if redirectToDateOfChange[RegisteredOffice](aboutTheBusiness.registeredOffice, data) =>
                  resultPromise success Redirect(routes.RegisteredOfficeDateOfChangeController.get())
                case _ => edit match {
                  case true => resultPromise success Redirect(routes.SummaryController.get())
                  case _ =>
                    auditConnector.sendEvent(AddressCreatedEvent(data)) map { _ =>
                      resultPromise success Redirect(routes.ContactingYouController.get(edit))
                    }
                }
              }
            }
        }

        resultPromise.future
  }

}

object RegisteredOfficeController extends RegisteredOfficeController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val statusService = StatusService
  override lazy val auditConnector = AMLSAuditConnector
}
