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

package controllers.responsiblepeople

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.responsiblepeople.{ContactDetails, ResponsiblePerson}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.contact_details

import scala.concurrent.Future

class ContactDetailsController @Inject () (
                                      override val dataCacheConnector: DataCacheConnector,
                                      override val authConnector: AuthConnector
                                          ) extends RepeatingSection with BaseController {


  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = Authorised.async {
      implicit authContext => implicit request =>
        getData[ResponsiblePerson](index) map {
          case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_, Some(name),_,_,_,_,_,_,_,_,_,_,_,_,_))
          => Ok(contact_details(Form2[ContactDetails](name), edit, index, flow, personName.titleName))
          case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_))
          => Ok(contact_details(EmptyForm, edit, index, flow, personName.titleName))
          case _ => NotFound(notFoundView)
        }
    }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) =
    Authorised.async {
      implicit authContext => implicit request => {

        Form2[ContactDetails](request.body) match {
          case f: InvalidForm =>
            getData[ResponsiblePerson](index) map { rp =>
              BadRequest(views.html.responsiblepeople.contact_details(f, edit, index, flow, ControllerHelper.rpTitleName(rp)))
            }
          case ValidForm(_, data) => {
            for {
              _ <- updateDataStrict[ResponsiblePerson](index) { rp =>
                rp.contactDetails(data)
              }
            } yield edit match {
              case true => Redirect(routes.DetailedAnswersController.get(index, flow))
              case false if index > 1 => Redirect(routes.CurrentAddressController.get(index, edit, flow))
              case false if index == 1 => Redirect(routes.ConfirmAddressController.get(index))
            }
          }.recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
      }
    }
}