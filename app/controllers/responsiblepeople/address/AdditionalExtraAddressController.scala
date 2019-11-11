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

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.DefaultBaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.responsiblepeople._
import services.AutoCompleteService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.address.additional_extra_address

import scala.concurrent.Future

class AdditionalExtraAddressController @Inject()(
                                                   val dataCacheConnector: DataCacheConnector,
                                                   authAction: AuthAction,
                                                   autoCompleteService: AutoCompleteService
                                                 ) extends RepeatingSection with DefaultBaseController {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
    implicit request =>
      getData[ResponsiblePerson](request.credId, index) map {
        case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,Some(ResponsiblePersonAddressHistory(_,_,Some(additionalExtraAddress))),_,_,_,_,_,_,_,_,_,_,_, _)) =>
          Ok(additional_extra_address(Form2[ResponsiblePersonAddress](additionalExtraAddress), edit, index, flow, personName.titleName))
        case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_)) =>
          Ok(additional_extra_address(EmptyForm, edit, index, flow, personName.titleName))
        case _ => NotFound(notFoundView)
      }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) =
    authAction.async {
      implicit request =>
        def processForm(data: ResponsiblePersonAddress) = {
          data.personAddress match {
            case _: PersonAddressUK => Future.successful(Redirect(routes.AdditionalExtraAddressUKController.get(index, edit, flow)))
            case _: PersonAddressNonUK => Future.successful(Redirect(routes.AdditionalExtraAddressNonUKController.get(index, edit, flow)))
          }
        }

        (Form2[ResponsiblePersonAddress](request.body) match {
          case f: InvalidForm if f.data.get("isUK").isDefined => processForm(ResponsiblePersonAddress(AddressHelper.modelFromForm(f), None))
          case f: InvalidForm =>
            getData[ResponsiblePerson](request.credId, index) map { rp =>
              BadRequest(additional_extra_address(f, edit, index, flow, ControllerHelper.rpTitleName(rp)))
            }
          case ValidForm(_, data) => {
            processForm(data)
          }}).recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
    }
}