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
import forms.{Form2, InvalidForm, ValidForm}
import models.aboutthebusiness.{AboutTheBusiness, CorrespondenceAddress, UKCorrespondenceAddress}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.aboutthebusiness._
import audit.AddressConversions._

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
          (for {
            aboutTheBusiness <- dataConnector.fetch[AboutTheBusiness](AboutTheBusiness.key)
            _ <- dataConnector.save[AboutTheBusiness](AboutTheBusiness.key,
              aboutTheBusiness.correspondenceAddress(data)
            )
          } yield edit match {
            case true => Future.successful(Redirect(routes.SummaryController.get()))
            case _ =>
              auditConnector.sendEvent(AddressCreatedEvent(data)) map { _ =>
                Redirect(routes.SummaryController.get())
              }
          }).flatMap(identity)
      }
    }
  }
}

object CorrespondenceAddressController extends CorrespondenceAddressController {
  // $COVERAGE-OFF$
  override protected val dataConnector: DataCacheConnector = DataCacheConnector
  override protected val authConnector: AuthConnector = AMLSAuthConnector
  override protected[controllers] lazy val auditConnector = AMLSAuditConnector
}
