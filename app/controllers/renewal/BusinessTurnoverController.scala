/*
 * Copyright 2024 HM Revenue & Customs
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
import forms.renewal.BusinessTurnoverFormProvider
import models.renewal.Renewal
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.RenewalService
import utils.AuthAction
import views.html.renewal.BusinessTurnoverView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class BusinessTurnoverController @Inject()(val dataCacheConnector: DataCacheConnector,
                                           val authAction: AuthAction,
                                           val ds: CommonPlayDependencies,
                                           val renewalService: RenewalService,
                                           val cc: MessagesControllerComponents,
                                           formProvider: BusinessTurnoverFormProvider,
                                           view: BusinessTurnoverView) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request =>
      renewalService.getRenewal(request.credId).map {
        response =>
          val form = (for {
            renewal <- response
            businessTurnover <- renewal.businessTurnover
          } yield formProvider().fill(businessTurnover)).getOrElse(formProvider())
          Ok(view(form, edit))
      }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request => {
      formProvider().bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, edit))),
        data =>
          for {
            renewal <- dataCacheConnector.fetch[Renewal](request.credId, Renewal.key)
            _ <- renewalService.updateRenewal(request.credId, renewal.businessTurnover(data))
          } yield if (edit) {
            Redirect(routes.SummaryController.get)
          } else {
            Redirect(routes.AMLSTurnoverController.get())
          }
      )
    }
  }
}
