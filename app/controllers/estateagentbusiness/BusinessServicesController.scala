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

package controllers.estateagentbusiness

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.businessmatching.{EstateAgentBusinessService => EAB}
import models.estateagentbusiness.{EstateAgentBusiness, Residential, Services}
import services.StatusService
import services.businessmatching.ServiceFlow
import utils.{AuthAction, DateOfChangeHelper}
import views.html.estateagentbusiness.business_servicess

import scala.concurrent.Future

@Singleton
class BusinessServicesController @Inject()(val dataCacheConnector: DataCacheConnector,
                                            val statusService: StatusService,
                                            val serviceFlow: ServiceFlow,
                                            authAction: AuthAction, val ds: CommonPlayDependencies
                                          ) extends AmlsBaseController(ds) with DateOfChangeHelper {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[EstateAgentBusiness](request.credId, EstateAgentBusiness.key) map {
        response =>
          val form = (for {
            estateAgentBusiness <- response
            accountant <- estateAgentBusiness.services
          } yield Form2[Services](accountant)).getOrElse(EmptyForm)
          Ok(business_servicess(form, edit))
      }
  }

  private def redirectToNextPage(edit: Boolean, data: Services) = {
    if(data.services.contains(Residential)) {
      Redirect(routes.ResidentialRedressSchemeController.get(edit))
    } else {
      if(edit) {
        Redirect(routes.SummaryController.get())
      } else  {
        Redirect(routes.PenalisedUnderEstateAgentsActController.get())
      }
    }
  }

  def updateData(business: EstateAgentBusiness, data: Services) : EstateAgentBusiness = {
    if(data.services.contains(Residential)) {
      business
    } else {
      business.copy(redressScheme = None)
    }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request =>
    import jto.validation.forms.Rules._
      Form2[Services](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(business_servicess(f, edit)))
        case ValidForm(_, data) =>
          for {
            estateAgentBusiness <- dataCacheConnector.fetch[EstateAgentBusiness](request.credId, EstateAgentBusiness.key)
            _ <- dataCacheConnector.save[EstateAgentBusiness](request.credId, EstateAgentBusiness.key, updateData(estateAgentBusiness.services(data), data))
            status <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
            isNewActivity <- serviceFlow.isNewActivity(request.credId, EAB)
          } yield {
            if (!isNewActivity & redirectToDateOfChange[Services](status, estateAgentBusiness.services, data)) {
              Redirect(routes.ServicesDateOfChangeController.get())
            } else {
              redirectToNextPage(edit, data)
            }
          }
      }
  }
}
