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

package controllers.renewal

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.renewal.{BusinessTurnover, Renewal}
import play.api.mvc.MessagesControllerComponents
import services.RenewalService
import utils.AuthAction

import views.html.renewal.business_turnover

import scala.concurrent.Future

@Singleton
class BusinessTurnoverController @Inject()(val dataCacheConnector: DataCacheConnector,
                                           val authAction: AuthAction,
                                           val ds: CommonPlayDependencies,
                                           val renewalService: RenewalService,
                                           val cc: MessagesControllerComponents,
                                           business_turnover: business_turnover) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      renewalService.getRenewal(request.credId).map {
        response =>
          val form: Form2[BusinessTurnover] = (for {
            renewal <- response
            businessTurnover <- renewal.businessTurnover
          } yield Form2[BusinessTurnover](businessTurnover)).getOrElse(EmptyForm)
          Ok(business_turnover(form, edit))
      }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request => {
      Form2[BusinessTurnover](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(business_turnover(f, edit)))
        case ValidForm(_, data) =>
          for {
            renewal <- dataCacheConnector.fetch[Renewal](request.credId, Renewal.key)
            _ <- renewalService.updateRenewal(request.credId, renewal.businessTurnover(data))
          } yield edit match {
            case true => Redirect(routes.SummaryController.get)
            case false => Redirect(routes.AMLSTurnoverController.get())
          }
      }
    }
  }
}
