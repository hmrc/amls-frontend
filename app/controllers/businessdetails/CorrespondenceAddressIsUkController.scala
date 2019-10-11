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
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessdetails.{BusinessDetails, CorrespondenceAddress, CorrespondenceAddressIsUk}
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.AuthAction
import scala.concurrent.ExecutionContext.Implicits.global
import views.html.businessdetails._

import scala.concurrent.Future

class CorrespondenceAddressIsUkController @Inject ()(val dataConnector: DataCacheConnector,
                                                     val auditConnector: AuditConnector,
                                                     val authAction: AuthAction,
                                                     val ds: CommonPlayDependencies,
                                                     val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataConnector.fetch[BusinessDetails](request.credId, BusinessDetails.key) map {
        response => {
          response.flatMap(businessDetails =>
            businessDetails.correspondenceAddressIsUk.map(isUk => isUk.isUk)
              .orElse(businessDetails.correspondenceAddress.flatMap(ca => ca.isUk)))
            .map(isUk => Ok(correspondence_address_is_uk(Form2[CorrespondenceAddressIsUk](CorrespondenceAddressIsUk(isUk)), edit)) )
            .getOrElse(Ok(correspondence_address_is_uk(EmptyForm, edit)))
        }
      }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request => {
      Form2[CorrespondenceAddressIsUk](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(correspondence_address_is_uk(f, edit)))
        case ValidForm(_, isUk) =>
          for {
            businessDetails <- dataConnector.fetch[BusinessDetails](request.credId, BusinessDetails.key)
            _ <- dataConnector.save[BusinessDetails](request.credId, BusinessDetails.key, businessDetails.correspondenceAddressIsUk(isUk))
            _ <- if (isUkHasChanged(businessDetails.correspondenceAddress, isUk = isUk)) { dataConnector.save[BusinessDetails](request.credId, BusinessDetails.key,
              businessDetails.correspondenceAddress(CorrespondenceAddress(None, None))) } else { Future.successful(None) }
          } yield isUk match {
            case CorrespondenceAddressIsUk(true) => Redirect(routes.CorrespondenceAddressUkController.get(edit))
            case CorrespondenceAddressIsUk(false) => Redirect(routes.CorrespondenceAddressNonUkController.get(edit))
          }
      }
    }
  }

  def isUkHasChanged(address: Option[CorrespondenceAddress], isUk: CorrespondenceAddressIsUk):Boolean = {
    (address, isUk) match {
      case (Some(CorrespondenceAddress(Some(_), None)), CorrespondenceAddressIsUk(false)) => true
      case (Some(CorrespondenceAddress(None, Some(_))), CorrespondenceAddressIsUk(true)) => true
      case _ => false
    }
  }
}