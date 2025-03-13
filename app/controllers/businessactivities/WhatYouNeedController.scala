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

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import models.businessmatching.BusinessMatching
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.AuthConnector
import utils.AuthAction
import views.html.businessactivities.WhatYouNeedView

import javax.inject.Inject

class WhatYouNeedController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authConnector: AuthConnector,
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  view: WhatYouNeedView
) extends AmlsBaseController(ds, cc)
    with Logging {

  def get: Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key) map { businessMatching =>
      (for {
        bm <- businessMatching
        ba <- bm.activities
      } yield Ok(view(Some(ba)))).getOrElse {
        val errorMessage = "Unable to retrieve business activities in [businessactivities][WhatYouNeedController]"
        logger.warn(errorMessage)
        throw new Exception(errorMessage)
      }
    }
  }
}
