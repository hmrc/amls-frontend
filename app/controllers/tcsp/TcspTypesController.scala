/*
 * Copyright 2022 HM Revenue & Customs
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

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.tcsp.{CompanyFormationAgent, RegisteredOfficeEtc, Tcsp, TcspTypes}
import play.api.mvc.MessagesControllerComponents
import utils.AuthAction
import views.html.tcsp.service_provider_types

import scala.concurrent.Future

class TcspTypesController @Inject()(val dataCacheConnector: DataCacheConnector,
                                    val authAction: AuthAction,
                                    val ds: CommonPlayDependencies,
                                    val cc: MessagesControllerComponents,
                                    service_provider_types: service_provider_types) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>

      dataCacheConnector.fetch[Tcsp](request.credId, Tcsp.key) map {
        response =>
          val form: Form2[TcspTypes] = (for {
            tcsp <- response
            tcspTypes <- tcsp.tcspTypes
          } yield Form2[TcspTypes](tcspTypes)).getOrElse(EmptyForm)
          Ok(service_provider_types(form, edit))
      }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request =>
      Form2[TcspTypes](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(service_provider_types(f, edit)))
        case ValidForm(_, data) => {
          val companyFormOrRegisteredOffice = (data.serviceProviders.contains(CompanyFormationAgent), data.serviceProviders.contains(RegisteredOfficeEtc))

          val result = for {
            tcsp <- dataCacheConnector.fetch[Tcsp](request.credId, Tcsp.key)
            cache <- dataCacheConnector.save[Tcsp](request.credId, Tcsp.key,
              {
                companyFormOrRegisteredOffice match {
                  case (false, false) => tcsp.tcspTypes(data).copy(onlyOffTheShelfCompsSold = None, complexCorpStructureCreation = None, providedServices = None)
                  case (false, true) => tcsp.tcspTypes(data).copy(onlyOffTheShelfCompsSold = None, complexCorpStructureCreation = None)
                  case (true, false) => tcsp.tcspTypes(data).copy(providedServices = None)
                  case (true, true) => tcsp.tcspTypes(data)
                }
              })
          } yield cache

          result map { _ =>
            companyFormOrRegisteredOffice match {
              case (true, _) => Redirect(routes.OnlyOffTheShelfCompsSoldController.get(edit))
              case (false, true) => Redirect(routes.ProvidedServicesController.get(edit))
              case _ => edit match {
                case true => Redirect(routes.SummaryController.get)
                case false => Redirect(routes.ServicesOfAnotherTCSPController.get())
              }
            }
          }
        }
      }
  }

}
