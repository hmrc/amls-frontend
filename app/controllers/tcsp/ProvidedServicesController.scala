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

package controllers.tcsp

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms._
import javax.inject.Inject
import models.tcsp.{ProvidedServices, Tcsp}
import play.api.mvc.MessagesControllerComponents
import utils.AuthAction
import scala.concurrent.ExecutionContext.Implicits.global
import views.html.tcsp.provided_services

import scala.concurrent.Future

class ProvidedServicesController @Inject() (val dataCacheConnector: DataCacheConnector,
                                            val authAction: AuthAction,
                                            val ds: CommonPlayDependencies,
                                            val cc: MessagesControllerComponents,
                                            provided_services: provided_services,
                                            implicit val error: views.html.error) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        dataCacheConnector.fetch[Tcsp](request.credId, Tcsp.key) map {
          response =>
            val form: Form2[ProvidedServices] = (for {
              tcsp <- response
              providedServices <- tcsp.providedServices
            } yield Form2[ProvidedServices](providedServices)).getOrElse(EmptyForm)
            Ok(provided_services(form, edit))
        }
  }

  def post(edit: Boolean = false) = authAction.async {
      implicit request =>
        Form2[ProvidedServices](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(provided_services(f, edit)))
          case ValidForm(_, data) =>
            for {
              tcsp <- dataCacheConnector.fetch[Tcsp](request.credId, Tcsp.key)
              _ <- dataCacheConnector.save[Tcsp](request.credId, Tcsp.key, tcsp.providedServices(data)
              )
            } yield edit match{
              case true => Redirect(routes.SummaryController.get())
              case false => Redirect(routes.ServicesOfAnotherTCSPController.get())
            }
        }
  }
}
