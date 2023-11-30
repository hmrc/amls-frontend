/*
 * Copyright 2023 HM Revenue & Customs
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
import forms.tcsp.ServiceProviderTypesFormProvider
import models.tcsp.Tcsp
import models.tcsp.TcspTypes.{CompanyFormationAgent, RegisteredOfficeEtc}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.AuthAction
import views.html.tcsp.ServiceProviderTypesView

import javax.inject.Inject
import scala.concurrent.Future

class TcspTypesController @Inject()(val dataCacheConnector: DataCacheConnector,
                                    val authAction: AuthAction,
                                    val ds: CommonPlayDependencies,
                                    val cc: MessagesControllerComponents,
                                    formProvider: ServiceProviderTypesFormProvider,
                                    view: ServiceProviderTypesView) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request =>

      dataCacheConnector.fetch[Tcsp](request.credId, Tcsp.key) map {
        response =>
          val form = (for {
            tcsp <- response
            tcspTypes <- tcsp.tcspTypes
          } yield formProvider().fill(tcspTypes)).getOrElse(formProvider())
          Ok(view(form, edit))
      }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request =>
      formProvider().bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, edit))),
        data => {
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
      )
  }

}
