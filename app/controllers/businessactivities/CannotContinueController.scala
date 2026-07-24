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
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import models.businessactivities.{BusinessActivities, RiskAssessmentHasPolicy}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.{AuthAction, ControllerHelper}
import views.html.businessactivities.CannotContinueView
import play.api.Logging

import scala.concurrent.Future

class CannotContinueController @Inject() (
                                           val dataCacheConnector: DataCacheConnector,
                                           val authAction: AuthAction,
                                           val ds: CommonPlayDependencies,
                                           val cc: MessagesControllerComponents,
                                           view: CannotContinueView,
                                           implicit val error: views.html.ErrorView
                                         ) extends AmlsBaseController(ds, cc) with Logging {

  def get: Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key) map { response =>
      (for {
        businessActivities   <- response
        riskAssessmentPolicy <- businessActivities.riskAssessmentPolicy
      } yield Ok(view())).getOrElse {
        val errorMessage = "Unable to retrieve business activities in [businessactivities][CannotContinueController]"
        logger.warn(errorMessage)
        throw new Exception(errorMessage)
      }
    }
  }
  
}
