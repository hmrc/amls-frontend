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

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.aboutthebusiness.{RegisteredOffice, AboutTheBusiness, LettersAddress}
import views.html.aboutthebusiness._

import scala.concurrent.Future

trait LettersAddressController extends BaseController {

  def dataCache: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetch[AboutTheBusiness](AboutTheBusiness.key) map {
        response =>
          val regOffice: Option[RegisteredOffice] = (for {
            aboutTheBusiness <- response
            registeredOffice <- aboutTheBusiness.registeredOffice
          } yield Option[RegisteredOffice](registeredOffice)).getOrElse(None)
          regOffice match {
            case Some(data) => Ok(letters_address(EmptyForm, data, edit))
            case _ => Redirect(routes.CorrespondenceAddressController.get())
          }
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[LettersAddress](request.body) match {
        case f: InvalidForm =>
          dataCache.fetch[AboutTheBusiness](AboutTheBusiness.key) map {
            response =>
              val regOffice: Option[RegisteredOffice] = (for {
                aboutTheBusiness <- response
                registeredOffice <- aboutTheBusiness.registeredOffice
              } yield Option[RegisteredOffice](registeredOffice)).getOrElse(None)
              regOffice match {
                case Some(data) => BadRequest(confirm_registered_office_or_main_place(f, data))
                case _ => Redirect(routes.RegisteredOfficeController.get(edit))
              }
          }
        case ValidForm(_, data) =>
          data.isRegOfficeOrMainPlaceOfBusiness match {
            case true => Future.successful(Redirect(routes.ContactingYouController.get(edit)))
            case false => Future.successful(Redirect(routes.RegisteredOfficeController.get(edit)))
          }
      }
  }
}

object LettersAddressController extends LettersAddressController {
  // $COVERAGE-OFF$
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
