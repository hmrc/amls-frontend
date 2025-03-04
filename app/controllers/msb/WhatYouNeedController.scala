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

package controllers.msb

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}

import javax.inject.Inject
import models.businessmatching.BusinessMatching
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StatusService
import utils.AuthAction
import views.html.msb.WhatYouNeedView

import scala.concurrent.Future

class WhatYouNeedController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val statusService: StatusService,
  val dataCacheConnector: DataCacheConnector,
  val cc: MessagesControllerComponents,
  view: WhatYouNeedView
) extends AmlsBaseController(ds, cc) {

  def get: Action[AnyContent] = authAction.async { implicit request =>
    val call = routes.ExpectedThroughputController.get()

    dataCacheConnector.fetchAll(request.credId) flatMap { optionalCache =>
      (for {
        cache            <- optionalCache
        businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
        bmMsbServices    <- businessMatching.msbServices
      } yield Future.successful(Ok(view(call, bmMsbServices)))) getOrElse Future.successful(Ok(view(call)))
    }
  }

  def post: Action[AnyContent] = authAction {
    Redirect(routes.ExpectedThroughputController.get())
  }
}
