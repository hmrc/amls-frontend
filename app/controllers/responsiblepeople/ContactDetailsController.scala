/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.responsiblepeople

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.responsiblepeople.{ContactDetails, ResponsiblePerson}
import play.api.mvc.MessagesControllerComponents
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.contact_details

import scala.concurrent.Future


class ContactDetailsController @Inject () (
                                           val dataCacheConnector: DataCacheConnector,
                                           authAction: AuthAction,
                                           val ds: CommonPlayDependencies,
                                           val cc: MessagesControllerComponents,
                                           contact_details: contact_details,
                                           implicit val error: views.html.error) extends AmlsBaseController(ds, cc) with RepeatingSection {


  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
      implicit request =>
        getData[ResponsiblePerson](request.credId, index) map {
          case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_, Some(name),_,_,_,_,_,_,_,_,_,_,_,_,_))
          => Ok(contact_details(Form2[ContactDetails](name), edit, index, flow, personName.titleName))
          case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_))
          => Ok(contact_details(EmptyForm, edit, index, flow, personName.titleName))
          case _ => NotFound(notFoundView)
        }
    }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) =
    authAction.async {
      implicit request => {
        Form2[ContactDetails](request.body) match {
          case f: InvalidForm =>
            getData[ResponsiblePerson](request.credId, index) map { rp =>
              BadRequest(contact_details(f, edit, index, flow, ControllerHelper.rpTitleName(rp)))
            }
          case ValidForm(_, data) => {
            for {
              _ <- updateDataStrict[ResponsiblePerson](request.credId, index) { rp =>
                rp.contactDetails(data)
              }
            } yield edit match {
              case true => Redirect(routes.DetailedAnswersController.get(index, flow))
              case false => Redirect(controllers.responsiblepeople.address.routes.CurrentAddressController.get(index, edit, flow))
            }
          }.recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
      }
    }
}