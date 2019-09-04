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
import javax.inject.Inject
import models.estateagentbusiness._
import utils.AuthAction
import views.html.estateagentbusiness._

import scala.concurrent.Future

class ResidentialRedressSchemeController  @Inject()(
                                                    val dataCacheConnector: DataCacheConnector,
                                                    authAction: AuthAction, val ds: CommonPlayDependencies) extends AmlsBaseController(ds) {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[EstateAgentBusiness](request.credId, EstateAgentBusiness.key) map {
        response =>
          val form = (for {
            estateAgentBusiness <- response
            redressScheme <- estateAgentBusiness.redressScheme
          } yield Form2[RedressScheme](redressScheme)).getOrElse(EmptyForm)
          Ok(redress_scheme(form, edit))
      }
  }

  def post(edit: Boolean = false) = authAction.async {
     implicit request =>
      Form2[RedressScheme](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(redress_scheme(f, edit)))
        case ValidForm(_, data) =>
          for {
            estateAgentBusiness <- dataCacheConnector.fetch[EstateAgentBusiness](request.credId, EstateAgentBusiness.key)
            _ <- dataCacheConnector.save[EstateAgentBusiness](request.credId, EstateAgentBusiness.key,
              estateAgentBusiness.redressScheme(data))
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.PenalisedUnderEstateAgentsActController.get())
          }
      }
  }
}