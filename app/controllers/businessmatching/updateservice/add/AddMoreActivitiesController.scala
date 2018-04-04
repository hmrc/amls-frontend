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

import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import controllers.businessmatching.updateservice.add.routes._
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.businessmatching.updateservice._
import models.businessmatching.{BusinessActivities, BusinessActivity}
import models.status.{NotCompleted, SubmissionReady}
import play.api.mvc.{Request, Result}
import services.StatusService
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

@Singleton
class AddMoreActivitiesController @Inject()(
                                           val authConnector: AuthConnector,
                                           val dataCacheConnector: DataCacheConnector,
                                           val statusService: StatusService,
                                           val businessMatchingService: BusinessMatchingService
                                         ) extends BaseController {

  def get(index: Int = 0) = Authorised.async {
    implicit authContext =>
      implicit request => Future.successful(Ok(views.html.businessmatching.updateservice.add_more_activities(EmptyForm)))

  }

  def post(index: Int = 0) = Authorised.async {
    implicit authContext =>
      implicit request => ???
//        Form2[AreNewActivitiesAtTradingPremises](request.body) match {
//        case ValidForm(_, data) => redirectTo(data, activities, index)
//        case f: InvalidForm => Future.successful(
//          BadRequest(views.html.businessmatching.updateservice.trading_premises(f, BusinessActivities.getValue(activity), index))
//        )
  }
}