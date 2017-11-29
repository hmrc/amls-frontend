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

package controllers.asp

import javax.inject.Inject

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.asp.{Asp, ServicesOfBusiness}
import models.businessmatching.AccountancyServices
import services.StatusService
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.DateOfChangeHelper
import views.html.asp._

import scala.concurrent.Future

class ServicesOfBusinessController @Inject()
(
  dataCacheConnector: DataCacheConnector,
  statusService: StatusService,
  val authConnector: AuthConnector,
  serviceFlow: ServiceFlow
) extends BaseController with DateOfChangeHelper {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[Asp](Asp.key) map {
        response =>
          val form = (for {
            business <- response
            setOfServices <- business.services
          } yield Form2[ServicesOfBusiness](setOfServices)).getOrElse(EmptyForm)
          Ok(services_of_business(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    import jto.validation.forms.Rules._
    implicit authContext => implicit request =>
      Form2[ServicesOfBusiness](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(services_of_business(f, edit)))
        case ValidForm(_, data) =>

          for {
            businessServices <- dataCacheConnector.fetch[Asp](Asp.key)
            _ <- dataCacheConnector.save[Asp](Asp.key,
              businessServices.services(data))
            status <- statusService.getStatus
            isNewActivity <- serviceFlow.isNewActivity(AccountancyServices)
          } yield {
            if (!isNewActivity && redirectToDateOfChange[ServicesOfBusiness](status, businessServices.services, data)) {
              Redirect(routes.ServicesOfBusinessDateOfChangeController.get())
            } else {
              edit match {
                case true => Redirect(routes.SummaryController.get())
                case false => Redirect(routes.OtherBusinessTaxMattersController.get(edit))
              }
            }
          }
      }
  }
}

