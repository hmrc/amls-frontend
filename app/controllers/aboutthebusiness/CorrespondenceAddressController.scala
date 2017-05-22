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
import forms.{Form2, InvalidForm, ValidForm}
import models.aboutthebusiness.{AboutTheBusiness, CorrespondenceAddress, RegisteredOffice, UKCorrespondenceAddress}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.aboutthebusiness._
import audit.AddressConversions._
import cats.data.OptionT
import cats.implicits._
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait CorrespondenceAddressController extends BaseController {

  protected def dataConnector: DataCacheConnector
  protected[controllers] val auditConnector: AuditConnector

  private val initialiseWithUK = UKCorrespondenceAddress("","", "", "", None, None, "")

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataConnector.fetch[AboutTheBusiness](AboutTheBusiness.key) map {
        response =>
          val form: Form2[CorrespondenceAddress] = (for {
            aboutTheBusiness <- response
            correspondenceAddress <- aboutTheBusiness.correspondenceAddress
          } yield Form2[CorrespondenceAddress](correspondenceAddress)).getOrElse(Form2[CorrespondenceAddress](initialiseWithUK))
          Ok(correspondence_address(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[CorrespondenceAddress](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(correspondence_address(f, edit)))
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
                        (implicit hc: HeaderCarrier): Future[AuditResult] = {
    if (edit) {
      auditConnector.sendEvent(AddressModifiedEvent(currentAddress, oldAddress))
    } else {
      auditConnector.sendEvent(AddressCreatedEvent(currentAddress))
    }
  }
}

object CorrespondenceAddressController extends CorrespondenceAddressController {
  // $COVERAGE-OFF$
  override protected val dataConnector: DataCacheConnector = DataCacheConnector
  override protected val authConnector: AuthConnector = AMLSAuthConnector
  override protected[controllers] lazy val auditConnector = AMLSAuditConnector
}
