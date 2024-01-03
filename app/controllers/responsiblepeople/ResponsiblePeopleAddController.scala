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
import models.responsiblepeople.ResponsiblePerson
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import utils.{AuthAction, RepeatingSection}


class ResponsiblePeopleAddController @Inject()(val dataCacheConnector: DataCacheConnector,
                                               authAction: AuthAction,
                                               val ds: CommonPlayDependencies,
                                               val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) with RepeatingSection {

  def get(displayGuidance: Boolean = true, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request => {
      addData[ResponsiblePerson](request.credId, ResponsiblePerson.default(None)).map { idx =>
        Redirect {
          flow match {
            case Some(_) => controllers.responsiblepeople.routes.WhatYouNeedController.get(idx, flow)
            case _ => redirectDependingOnGuidance(displayGuidance, idx, flow)
          }
        }
      }
    }
  }

  private def redirectDependingOnGuidance(displayGuidance: Boolean, idx: Int, flow: Option[String]): Call = {
    if (displayGuidance) {
      controllers.responsiblepeople.routes.WhoMustRegisterController.get(idx, flow)
    } else {
      controllers.responsiblepeople.routes.WhatYouNeedController.get(idx)
    }
  }
}