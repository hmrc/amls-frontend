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

package controllers.businessmatching

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.businessmatching.CompanyRegistrationNumberFormProvider
import models.businessmatching.BusinessMatching
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StatusService
import services.businessmatching.BusinessMatchingService
import utils.AuthAction
import views.html.businessmatching.CompanyRegistrationNumberView

import javax.inject.Inject
import scala.concurrent.Future

class CompanyRegistrationNumberController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val dataCacheConnector: DataCacheConnector,
  val statusService: StatusService,
  val businessMatchingService: BusinessMatchingService,
  val cc: MessagesControllerComponents,
  formProvider: CompanyRegistrationNumberFormProvider,
  view: CompanyRegistrationNumberView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    (for {
      bm     <- businessMatchingService.getModel(request.credId)
      status <- OptionT.liftF(statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId))
    } yield {
      val form = bm.companyRegistrationNumber.map(formProvider().fill)
      Ok(view(form.getOrElse(formProvider()), edit, bm.hasAccepted, statusService.isPreSubmission(status)))
    }) getOrElse Redirect(controllers.routes.RegistrationProgressController.get())
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    val form = formProvider().bindFromRequest()

    form.fold(
      formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit))),
      value =>
        for {
          businessMatching <- dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key)
          _                <- dataCacheConnector.save[BusinessMatching](
                                request.credId,
                                BusinessMatching.key,
                                businessMatching.companyRegistrationNumber(value)
                              )
        } yield
          if (edit) {
            Redirect(routes.SummaryController.get())
          } else {
            Redirect(routes.RegisterServicesController.get())
          }
    )
  }
}
