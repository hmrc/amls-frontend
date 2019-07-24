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

package controllers.businessactivities

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.DefaultBaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.{BusinessActivities, RiskAssessmentPolicy, RiskAssessmentTypes}
import models.businessmatching.BusinessMatching
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import utils.{AuthAction, ControllerHelper}
import views.html.businessactivities._

import scala.concurrent.Future

class DocumentRiskAssessmentController @Inject()(val dataCacheConnector: DataCacheConnector,
                                                 val authAction: AuthAction) extends DefaultBaseController {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[BusinessActivities](request.cacheId, BusinessActivities.key) map {
        response =>
          val form: Form2[RiskAssessmentTypes] = (for {
            businessActivities <- response
            riskAssessmentPolicy: RiskAssessmentPolicy <- businessActivities.riskAssessmentPolicy
          } yield Form2[RiskAssessmentTypes](riskAssessmentPolicy.riskassessments)).getOrElse(EmptyForm)
          Ok(document_risk_assessment_policy(form, edit))
      }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request =>
      import jto.validation.forms.Rules._
      Form2[RiskAssessmentTypes](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(document_risk_assessment_policy(f, edit)))
        case ValidForm(_, data: RiskAssessmentTypes) => {
          dataCacheConnector.fetchAll(request.cacheId) flatMap { maybeCache =>
            val businessMatching = for {
              cacheMap <- maybeCache
              bm <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
            } yield bm

            for {
              businessActivities <- dataCacheConnector.fetch[BusinessActivities](request.cacheId, BusinessActivities.key)
              _ <- dataCacheConnector.save[BusinessActivities](request.cacheId, BusinessActivities.key, businessActivities.riskAssessmentTypes(data))
            } yield redirectDependingOnEdit(edit, ControllerHelper.isAccountancyServicesSelected(Some(businessMatching)))

          }
        } recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
  }

  private def redirectDependingOnEdit(edit: Boolean, accountancyServices: Boolean) =
    if (!edit && !accountancyServices) {
      Redirect(routes.AccountantForAMLSRegulationsController.get())
  } else {
      Redirect(routes.SummaryController.get())
    }
}
