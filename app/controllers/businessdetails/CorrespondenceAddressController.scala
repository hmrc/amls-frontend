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
import forms.{Form2, InvalidForm, ValidForm}
import models.businessdetails.{AboutTheBusiness, CorrespondenceAddress, UKCorrespondenceAddress}
import play.api.mvc.Request
import services.AutoCompleteService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.aboutthebusiness._

import scala.concurrent.Future

class CorrespondenceAddressController @Inject () (
                                                 val dataConnector: DataCacheConnector,
                                                 val authConnector: AuthConnector,
                                                 val auditConnector: AuditConnector,
                                                 val autoCompleteService: AutoCompleteService
                                                 ) extends BaseController {



  private val initialiseWithUK = UKCorrespondenceAddress("","", "", "", None, None, "")

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataConnector.fetch[AboutTheBusiness](AboutTheBusiness.key) map {
        response =>
          val form: Form2[CorrespondenceAddress] = (for {
            aboutTheBusiness <- response
            correspondenceAddress <- aboutTheBusiness.correspondenceAddress
          } yield Form2[CorrespondenceAddress](correspondenceAddress)).getOrElse(Form2[CorrespondenceAddress](initialiseWithUK))
          Ok(correspondence_address(form, edit, autoCompleteService.getCountries))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[CorrespondenceAddress](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(correspondence_address(f, edit, autoCompleteService.getCountries)))
        case ValidForm(_, data) =>
          val doUpdate = for {
            aboutTheBusiness <- OptionT(dataConnector.fetch[AboutTheBusiness](AboutTheBusiness.key))
            _ <- OptionT.liftF(dataConnector.save[AboutTheBusiness](AboutTheBusiness.key, aboutTheBusiness.correspondenceAddress(data)))
            _ <- OptionT.liftF(auditAddressChange(data, aboutTheBusiness.correspondenceAddress, edit)) orElse OptionT.some(Success)
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case _ => Redirect(routes.SummaryController.get())
          }

          doUpdate getOrElse InternalServerError("Could not update correspondence address")
      }
    }
  }

  def auditAddressChange(currentAddress: CorrespondenceAddress, oldAddress: Option[CorrespondenceAddress], edit: Boolean)
                        (implicit hc: HeaderCarrier, request: Request[_]): Future[AuditResult] = {
    if (edit) {
      auditConnector.sendEvent(AddressModifiedEvent(currentAddress, oldAddress))
    } else {
      auditConnector.sendEvent(AddressCreatedEvent(currentAddress))
    }
  }
}