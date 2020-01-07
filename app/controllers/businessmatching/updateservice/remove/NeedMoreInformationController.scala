/*
 * Copyright 2020 HM Revenue & Customs
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
import controllers.DefaultBaseController
import javax.inject.{Inject, Singleton}
import models.businessmatching.BusinessActivity
import models.flowmanagement.{NeedToUpdatePageId, RemoveBusinessTypeFlowModel}
import services.flowmanagement.Router
import utils.AuthAction
import views.html.businessmatching.updateservice.remove.need_more_information

import scala.concurrent.Future

@Singleton
class NeedMoreInformationController @Inject()(authAction: AuthAction,
                                              implicit val dataCacheConnector: DataCacheConnector,
                                              val router: Router[RemoveBusinessTypeFlowModel]
                                             ) extends DefaultBaseController {

  def get() = authAction.async {
      implicit request =>
        (for {
          model <- OptionT(dataCacheConnector.fetch[RemoveBusinessTypeFlowModel](request.credId, RemoveBusinessTypeFlowModel.key))
          activities <- OptionT.fromOption[Future](model.activitiesToRemove) orElse OptionT.some[Future, Set[BusinessActivity]](Set.empty)
        } yield {
          val activityNames = activities map { _.getMessage() }
          Ok(need_more_information(activityNames))
         })getOrElse(InternalServerError("Cannot retrieve information from cache"))
  }

  def post() = authAction.async {
      implicit request =>
        (for {
            route <- OptionT.liftF(router.getRoute(request.credId, NeedToUpdatePageId, new RemoveBusinessTypeFlowModel()))
        } yield route) getOrElse InternalServerError("Post: Cannot retrieve data: Remove : NewServiceInformationController")
  }
}