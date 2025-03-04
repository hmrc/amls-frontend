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

package controllers.asp

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.asp.ServicesOfBusinessFormProvider
import models.asp.{Asp, ServicesOfBusiness}
import models.businessmatching.BusinessActivity.AccountancyServices
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StatusService
import services.businessmatching.ServiceFlow
import utils.{AuthAction, DateOfChangeHelper}
import views.html.asp.ServicesOfBusinessView

import javax.inject.Inject
import scala.concurrent.Future

class ServicesOfBusinessController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val statusService: StatusService,
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val serviceFlow: ServiceFlow,
  val cc: MessagesControllerComponents,
  formProvider: ServicesOfBusinessFormProvider,
  view: ServicesOfBusinessView
) extends AmlsBaseController(ds, cc)
    with DateOfChangeHelper {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[Asp](request.credId, Asp.key) map { response =>
      val form = (for {
        business      <- response
        setOfServices <- business.services
      } yield formProvider().fill(setOfServices)).getOrElse(formProvider())
      Ok(view(form, edit))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithError => Future.successful(BadRequest(view(formWithError, edit))),
        data =>
          for {
            businessServices <- dataCacheConnector.fetch[Asp](request.credId, Asp.key)
            _                <- dataCacheConnector.save[Asp](request.credId, Asp.key, businessServices.services(data))
            status           <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
            isNewActivity    <- serviceFlow.isNewActivity(request.credId, AccountancyServices)
          } yield
            if (!isNewActivity && redirectToDateOfChange[ServicesOfBusiness](status, businessServices.services, data)) {
              Redirect(routes.ServicesOfBusinessDateOfChangeController.get)
            } else {
              if (edit) {
                Redirect(routes.SummaryController.get)
              } else {
                Redirect(routes.OtherBusinessTaxMattersController.get(edit))
              }
            }
      )
  }
}
