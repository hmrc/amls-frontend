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

package controllers.responsiblepeople

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import models.businessmatching.BusinessMatching
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.AuthAction
import play.api.Logging
import views.html.responsiblepeople.WhatYouNeedView

class WhatYouNeedController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  view: WhatYouNeedView
) extends AmlsBaseController(ds, cc)
    with Logging {

  def get(index: Int, flow: Option[String] = None): Action[AnyContent] =
    authAction.async { implicit request =>
      dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key) map { businessMatching =>
        (for {
          bm <- businessMatching
          ba <- bm.activities
        } yield Ok(view(controllers.responsiblepeople.routes.PersonNameController.get(index, false, flow), Some(ba))))
          .getOrElse {
            logger.warn("Unable to retrieve business activities in [responsiblepeople][WhatYouNeedController]")
            throw new Exception("Unable to retrieve business activities in [responsiblepeople][WhatYouNeedController]")
          }
      }
    }
}
