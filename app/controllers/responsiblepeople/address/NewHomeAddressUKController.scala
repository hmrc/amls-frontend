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

import connectors.DataCacheConnector
import controllers.DefaultBaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.responsiblepeople.NewHomeAddress._
import models.responsiblepeople._
import services.AutoCompleteService
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.address.new_home_address_UK

import scala.concurrent.Future

@Singleton
class NewHomeAddressUKController @Inject()(authAction: AuthAction,
                                           val dataCacheConnector: DataCacheConnector) extends AddressHelper with DefaultBaseController {

  def get(index: Int) = authAction.async {
    implicit request =>
      getData[ResponsiblePerson](request.credId, index) map {
        case Some(ResponsiblePerson(Some(personName), _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _))
        => Ok(new_home_address_UK(EmptyForm, index, personName.titleName))
        case _
        => NotFound(notFoundView)
      }
  }

  def post(index: Int) =
    authAction.async {
      implicit request =>
        (Form2[NewHomeAddress](request.body) match {
          case f: InvalidForm =>
            getData[ResponsiblePerson](request.credId, index) map { rp =>
              BadRequest(new_home_address_UK(f, index, ControllerHelper.rpTitleName(rp)))
            }
          case ValidForm(_, data) => {
            for {
              moveDate <- dataCacheConnector.fetch[NewHomeDateOfChange](request.credId, NewHomeDateOfChange.key)
              _ <- updateDataStrict[ResponsiblePerson](request.credId, index) { rp =>
                rp.addressHistory(convertToCurrentAddress(data, moveDate, rp))
              }
              _ <- dataCacheConnector.save[NewHomeDateOfChange](request.credId, NewHomeDateOfChange.key, NewHomeDateOfChange(None))
              _ <- dataCacheConnector.removeByKey(request.credId, NewHomeAddress.key)
            } yield {
              Redirect(controllers.responsiblepeople.routes.DetailedAnswersController.get(index))
            }
          }
        }).recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
    }
}
