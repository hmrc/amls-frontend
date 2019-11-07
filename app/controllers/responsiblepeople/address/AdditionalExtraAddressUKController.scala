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

package controllers.responsiblepeople.address

import audit.AddressConversions._
import audit.{AddressCreatedEvent, AddressModifiedEvent}
import cats.data._
import cats.implicits._
import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.DefaultBaseController
import controllers.responsiblepeople.routes
import forms.{Form2, InvalidForm, ValidForm}
import models.responsiblepeople._
import play.api.mvc.{AnyContent, Request}
import services.AutoCompleteService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.additional_extra_address
import views.html.responsiblepeople.address.additional_extra_address_UK

import scala.concurrent.Future

class AdditionalExtraAddressUKController @Inject()(
                                                   val dataCacheConnector: DataCacheConnector,
                                                   authAction: AuthAction,
                                                   auditConnector: AuditConnector,
                                                   autoCompleteService: AutoCompleteService
                                                 ) extends RepeatingSection with DefaultBaseController {


  final val DefaultAddressHistory = ResponsiblePersonAddress(PersonAddressUK("", "", None, None, ""), None)

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
    implicit request =>
      getData[ResponsiblePerson](request.credId, index) map {
        case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,Some(ResponsiblePersonAddressHistory(_,_,Some(additionalExtraAddress))),_,_,_,_,_,_,_,_,_,_,_, _)) =>
          Ok(additional_extra_address_UK(Form2[ResponsiblePersonAddress](additionalExtraAddress), edit, index, flow, personName.titleName))
        case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_)) =>
          Ok(additional_extra_address_UK(Form2(DefaultAddressHistory), edit, index, flow, personName.titleName))
        case _ => NotFound(notFoundView)
      }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) =
    authAction.async {
      implicit request =>
        (Form2[ResponsiblePersonCurrentAddress](request.body) match {
          case f: InvalidForm =>
            Future.successful(Redirect(routes.TimeAtAdditionalExtraAddressController.get(index, edit, flow)))
          case ValidForm(_, data) => {
            Future.successful(Redirect(routes.TimeAtAdditionalExtraAddressController.get(index, edit, flow)))
          }}).recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
    }
}