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

package controllers.businessmatching.updateservice.remove

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import javax.inject.Inject
import models.flowmanagement.RemoveServiceFlowModel
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessmatching.updateservice.remove.remove_activities_summary

import scala.concurrent.Future

class RemoveActivitiesSummaryController @Inject()(
                                                 val authConnector: AuthConnector,
                                                 val dataCacheConnector: DataCacheConnector
                                               ) extends BaseController {

  def get = Authorised.async{
    implicit authContext =>
      implicit request => {
        for {
          flow <- OptionT(dataCacheConnector.fetch[RemoveServiceFlowModel](RemoveServiceFlowModel.key))
        } yield Ok(remove_activities_summary(flow))
      } getOrElse InternalServerError("Could not access the flow model")
  }

  def post = Authorised.async{
    implicit authContext =>
      implicit request => ???
  }
}
