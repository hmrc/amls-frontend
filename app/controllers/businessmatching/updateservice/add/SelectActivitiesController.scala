/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.businessmatching.updateservice.add

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, InvalidForm}
import javax.inject.{Inject, Singleton}
import models.businessmatching.BusinessActivity._
import models.businessmatching.{BusinessActivities => BusinessMatchingActivities}
import models.flowmanagement.AddServiceFlowModel
import services.StatusService
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection
import views.html.businessmatching.updateservice.select_activities

import scala.concurrent.ExecutionContext

@Singleton
class SelectActivitiesController @Inject()(val authConnector: AuthConnector,
                                           val statusService: StatusService,
                                           val dataCacheConnector: DataCacheConnector,
                                           val businessMatchingService: BusinessMatchingService) extends BaseController with RepeatingSection {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        (for {
          model <- OptionT(dataCacheConnector.fetch[AddServiceFlowModel](AddServiceFlowModel.key)) orElse OptionT.some(AddServiceFlowModel())
          (names, values) <- getFormData
        } yield {
          val form = Form2(model.businessActivities.getOrElse(BusinessMatchingActivities(Set.empty)))

          Ok(select_activities(form, edit, values, names, false))
        }) getOrElse InternalServerError("Failed to get activities")
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        import jto.validation.forms.Rules._

        Form2[BusinessMatchingActivities](request.body) match {
          case f: InvalidForm => getFormData map {
            case (names, values) =>
              BadRequest(select_activities(f, edit, values, names, false))
          } getOrElse InternalServerError("Could not get form data")
        }
  }

  private def getFormData(implicit ac: AuthContext, hc: HeaderCarrier) = for {
    existing <- businessMatchingService.getSubmittedBusinessActivities
  } yield {

    val existingActivityNames = existing map {
      _.getMessage
    }

    val activityValues = (BusinessMatchingActivities.all diff existing) map BusinessMatchingActivities.getValue

    (existingActivityNames, activityValues)
  }

}