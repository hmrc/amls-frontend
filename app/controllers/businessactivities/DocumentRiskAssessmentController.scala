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

package controllers.businessactivities

import com.google.inject.Inject
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.businessactivities.DocumentRiskAssessmentPolicyFormProvider
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.businessactivities.DocumentRiskAssessmentService
import utils.{AuthAction, ControllerHelper}
import views.html.businessactivities.DocumentRiskAssessmentPolicyView

import scala.concurrent.Future

class DocumentRiskAssessmentController @Inject() (
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  service: DocumentRiskAssessmentService,
  formProvider: DocumentRiskAssessmentPolicyFormProvider,
  view: DocumentRiskAssessmentPolicyView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    service.getRiskAssessmentPolicy(request.credId) map { responseOpt =>
      val form = responseOpt.fold(formProvider())(x => formProvider().fill(x.riskassessments))
      Ok(view(form, edit))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit))),
        data =>
          {
            service
              .updateRiskAssessmentType(request.credId, data)
              .map(_.map { bm =>
                redirectDependingOnEdit(edit, ControllerHelper.isAccountancyServicesSelected(bm))
              })
              .map(_.head)
          } recoverWith { case _: IndexOutOfBoundsException =>
            Future.successful(NotFound(notFoundView))
          }
      )
  }

  private def redirectDependingOnEdit(edit: Boolean, accountancyServices: Boolean): Result =
    if (!edit && !accountancyServices) {
      Redirect(routes.AccountantForAMLSRegulationsController.get())
    } else {
      Redirect(routes.SummaryController.get)
    }
}
