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

package controllers.asp

import connectors.DataCacheConnector
import controllers.{BaseController, DefaultBaseController}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.asp.{Asp, ServicesOfBusiness}
import models.businessmatching.AccountancyServices
import services.StatusService
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthAction, DateOfChangeHelper}
import views.html.asp._

import scala.concurrent.Future

class ServicesOfBusinessController @Inject()(val dataCacheConnector: DataCacheConnector,
                                             val statusService: StatusService,
                                             authAction: AuthAction,
                                             val serviceFlow: ServiceFlow
                                            ) extends DefaultBaseController with DateOfChangeHelper {

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        dataCacheConnector.fetch[Asp](request.cacheId, Asp.key) map {
          response =>
            val form = (for {
              business <- response
              setOfServices <- business.services
            } yield Form2[ServicesOfBusiness](setOfServices)).getOrElse(EmptyForm)
            Ok(services_of_business(form, edit))
        }
  }

  def post(edit: Boolean = false) = authAction.async {
    import jto.validation.forms.Rules._
      implicit request =>
        Form2[ServicesOfBusiness](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(services_of_business(f, edit)))
          case ValidForm(_, data) =>

            for {
              businessServices <- dataCacheConnector.fetch[Asp](request.cacheId, Asp.key)
              _ <- dataCacheConnector.save[Asp](request.cacheId, Asp.key,
                businessServices.services(data))
              status <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.cacheId)
              isNewActivity <- serviceFlow.isNewActivity(request.cacheId, AccountancyServices)
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

