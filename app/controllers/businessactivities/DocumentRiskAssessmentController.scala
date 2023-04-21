/*
 * Copyright 2023 HM Revenue & Customs
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
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.businessactivities.DocumentRiskAssessmentPolicyFormProvider
import models.businessactivities.{BusinessActivities, RiskAssessmentPolicy}
import models.businessmatching.BusinessMatching
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.{AuthAction, ControllerHelper}
import views.html.businessactivities.DocumentRiskAssessmentPolicyView

import scala.concurrent.Future


class DocumentRiskAssessmentController @Inject()(val dataCacheConnector: DataCacheConnector,
                                                 val authAction: AuthAction,
                                                 val ds: CommonPlayDependencies,
                                                 val cc: MessagesControllerComponents,
                                                 formProvider: DocumentRiskAssessmentPolicyFormProvider,
                                                 view: DocumentRiskAssessmentPolicyView,
                                                 implicit val error: views.html.error) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key) map {
        response =>
          val form = (for {
            businessActivities <- response
            riskAssessmentPolicy: RiskAssessmentPolicy <- businessActivities.riskAssessmentPolicy
          } yield formProvider().fill(riskAssessmentPolicy.riskassessments)).getOrElse(formProvider())
          Ok(view(form, edit))
      }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request =>

      formProvider().bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, edit))),
        data => {
          dataCacheConnector.fetchAll(request.credId) flatMap { maybeCache =>
            val businessMatching = for {
              cacheMap <- maybeCache
              bm <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
            } yield bm

            for {
              businessActivities <- dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key)
              _ <- dataCacheConnector.save[BusinessActivities](request.credId, BusinessActivities.key, businessActivities.riskAssessmentTypes(data))
            } yield redirectDependingOnEdit(edit, ControllerHelper.isAccountancyServicesSelected(businessMatching))

          }
        } recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      )
  }

  private def redirectDependingOnEdit(edit: Boolean, accountancyServices: Boolean) =
    if (!edit && !accountancyServices) {
      Redirect(routes.AccountantForAMLSRegulationsController.get())
  } else {
      Redirect(routes.SummaryController.get)
    }
}
