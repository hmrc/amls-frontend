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

package controllers.businessactivities

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.{BusinessActivities, RiskAssessmentHasPolicy}
import models.businessmatching.BusinessMatching
import play.api.mvc.MessagesControllerComponents
import utils.{AuthAction, ControllerHelper}
import views.html.businessactivities._

import scala.concurrent.Future


class RiskAssessmentController @Inject() (val dataCacheConnector: DataCacheConnector,
                                          val authAction: AuthAction,
                                          val ds: CommonPlayDependencies,
                                          val cc: MessagesControllerComponents,
                                          risk_assessment_policy: risk_assessment_policy,
                                          implicit val error: views.html.error) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key) map {
        response =>
          val form: Form2[RiskAssessmentHasPolicy] = (for {
            businessActivities <- response
            riskAssessmentPolicy <- businessActivities.riskAssessmentPolicy
          } yield Form2[RiskAssessmentHasPolicy](riskAssessmentPolicy.hasPolicy)).getOrElse(EmptyForm)
          Ok(risk_assessment_policy(form, edit))
      }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request =>

      Form2[RiskAssessmentHasPolicy](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(risk_assessment_policy(f, edit)))
        case ValidForm(_, data: RiskAssessmentHasPolicy) => {
          dataCacheConnector.fetchAll(request.credId) flatMap { maybeCache =>
            val businessMatching = for {
              cacheMap <- maybeCache
              bm <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
            } yield bm

            for {
              businessActivities <- dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key)
              _ <- dataCacheConnector.save[BusinessActivities](request.credId, BusinessActivities.key, businessActivities.riskAssessmentHasPolicy(data))
            } yield redirectDependingOnAccountancyServices(ControllerHelper.isAccountancyServicesSelected(Some(businessMatching)), data)
          }
        } recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
  }

  private def redirectDependingOnAccountancyServices(accountancyServices: Boolean, data: RiskAssessmentHasPolicy) =
    accountancyServices match {
      case _ if data == RiskAssessmentHasPolicy(true) => Redirect(routes.DocumentRiskAssessmentController.get())
      case true => Redirect(routes.SummaryController.get)
      case false => Redirect(routes.AccountantForAMLSRegulationsController.get())
    }
}
