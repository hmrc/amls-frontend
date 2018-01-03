/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.tcsp

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, EmptyForm, Form2}
import models.tcsp.{RegisteredOfficeEtc, TcspTypes, Tcsp}
import views.html.tcsp.service_provider_types

import scala.concurrent.Future

trait TcspTypesController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[Tcsp](Tcsp.key) map {
        response =>
          val form: Form2[TcspTypes] = (for {
            tcsp <- response
            tcspTypes <- tcsp.tcspTypes
          } yield Form2[TcspTypes](tcspTypes)).getOrElse(EmptyForm)
          Ok(service_provider_types(form, edit))
      }
  }

  def post(edit : Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[TcspTypes](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(service_provider_types(f, edit)))
        case ValidForm(_, data) => {
          for {
            tcsp <-
            dataCacheConnector.fetch[Tcsp](Tcsp.key)
            _ <- dataCacheConnector.save[Tcsp](Tcsp.key,
              tcsp.tcspTypes(data)
            )
          } yield data.serviceProviders.contains(RegisteredOfficeEtc) match {
            case true => Redirect(routes.ProvidedServicesController.get(edit))
            case false => edit match {
                case true => Redirect(routes.SummaryController.get())
                case false => Redirect(routes.ServicesOfAnotherTCSPController.get())
            }
          }
        }
      }
  }
}

object TcspTypesController extends TcspTypesController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
