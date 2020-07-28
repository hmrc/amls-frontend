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

package controllers.responsiblepeople.address

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.responsiblepeople.NewHomeAddress._
import models.responsiblepeople._
import play.api.mvc.MessagesControllerComponents
import services.AutoCompleteService
import utils.{AuthAction, ControllerHelper}
import views.html.responsiblepeople.address.new_home_address_NonUK

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class NewHomeAddressNonUKController @Inject()(authAction: AuthAction,
                                              val dataCacheConnector: DataCacheConnector,
                                              val autoCompleteService: AutoCompleteService,
                                              val ds: CommonPlayDependencies,
                                              val cc: MessagesControllerComponents,
                                              new_home_address_NonUK: new_home_address_NonUK,
                                              implicit val error: views.html.error) extends AmlsBaseController(ds, cc) with AddressHelper {

  def get(index: Int) = authAction.async {
    implicit request =>
      getData[ResponsiblePerson](request.credId, index) map {
        case Some(ResponsiblePerson(Some(personName), _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _))
        => Ok(new_home_address_NonUK(EmptyForm, index, personName.titleName, autoCompleteService.getCountries))
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
              BadRequest(new_home_address_NonUK(f, index, ControllerHelper.rpTitleName(rp), autoCompleteService.getCountries))
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
