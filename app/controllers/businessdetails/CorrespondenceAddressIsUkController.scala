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

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, InvalidForm, ValidForm}
import models.businessdetails.{BusinessDetails, CorrespondenceAddressIsUk}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessdetails._

import scala.concurrent.Future

class CorrespondenceAddressIsUkController @Inject ()(
                                                 val dataConnector: DataCacheConnector,
                                                 val authConnector: AuthConnector,
                                                 val auditConnector: AuditConnector
                                                 ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataConnector.fetch[BusinessDetails](BusinessDetails.key) map {
        response =>
          val form: Form2[CorrespondenceAddressIsUk] = (for {
            businessDetails <- response
            correspondenceAddress <- businessDetails.correspondenceAddress
          } yield Form2[CorrespondenceAddressIsUk](CorrespondenceAddressIsUk(correspondenceAddress.isUk))).getOrElse(Form2[CorrespondenceAddressIsUk](CorrespondenceAddressIsUk(None)))
          Ok(correspondence_address_is_uk(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[CorrespondenceAddressIsUk](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(correspondence_address_is_uk(f, edit)))
        case f: InvalidForm => Future.successful(BadRequest(correspondence_address_is_uk(f, edit)))
        case ValidForm(_, CorrespondenceAddressIsUk(Some(true))) => Future.successful(Redirect(routes.CorrespondenceAddressIsUkController.get(edit)))
        case ValidForm(_, CorrespondenceAddressIsUk(Some(false))) => Future.successful(Redirect(routes.CorrespondenceAddressNonUkController.get(edit)))
      }
    }
  }
}