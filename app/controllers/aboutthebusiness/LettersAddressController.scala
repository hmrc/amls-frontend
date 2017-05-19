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
import forms._
import models.aboutthebusiness.{RegisteredOffice, AboutTheBusiness, LettersAddress}
import views.html.aboutthebusiness._

import scala.concurrent.Future

trait LettersAddressController extends BaseController {

  def dataCache: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      for {
        aboutTheBusiness <-
        dataCache.fetch[AboutTheBusiness](AboutTheBusiness.key)
      } yield aboutTheBusiness match {
        case Some(AboutTheBusiness(_,_, _, _, _, Some(registeredOffice), None, _)) =>
          Ok(letters_address(EmptyForm, registeredOffice, edit))
        case _ =>
          Redirect(routes.CorrespondenceAddressController.get(edit))
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
                case Some(data) => BadRequest(letters_address(f, data))
                case _ => Redirect(routes.CorrespondenceAddressController.get(edit))
              }
          }
        case ValidForm(_, data) =>
          data.lettersAddress match {
            case true => Future.successful(Redirect(routes.SummaryController.get()))
            case false => Future.successful(Redirect(routes.CorrespondenceAddressController.get(edit)))
          }
      }
  }
}

object LettersAddressController extends LettersAddressController {
  // $COVERAGE-OFF$
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
