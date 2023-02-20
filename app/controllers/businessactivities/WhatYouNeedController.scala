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

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import javax.inject.Inject
import models.businessmatching.BusinessMatching
import play.api.mvc.MessagesControllerComponents
import services.StatusService
import uk.gov.hmrc.auth.core.AuthConnector
import utils.AuthAction
import views.html.businessactivities._
import play.api.Logging

class WhatYouNeedController @Inject()(val dataCacheConnector: DataCacheConnector,
                                      val authConnector: AuthConnector,
                                      statusService: StatusService,
                                      authAction: AuthAction,
                                      val ds: CommonPlayDependencies,
                                      val cc: MessagesControllerComponents,
                                      what_you_need: what_you_need) extends AmlsBaseController(ds, cc) with Logging {

  def get = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key) map { businessMatching =>
        (for {
          bm <- businessMatching
          ba <- bm.activities
        } yield {
          Ok(what_you_need(routes.InvolvedInOtherController.get().url, Some(ba)))
        }).getOrElse {
            logger.info("Unable to retrieve business activities in [businessactivities][WhatYouNeedController]")
            throw new Exception("Unable to retrieve business activities in [businessactivities][WhatYouNeedController]")
          }
      }
  }
}