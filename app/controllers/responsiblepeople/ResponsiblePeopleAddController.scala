/*
 * Copyright 2019 HM Revenue & Customs
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
import controllers.BaseController
import models.responsiblepeople.ResponsiblePerson
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection

class ResponsiblePeopleAddController @Inject () (
                                                val dataCacheConnector: DataCacheConnector,
                                                val authConnector: AuthConnector
                                                ) extends BaseController with RepeatingSection {

  def get(displayGuidance: Boolean = true, flow: Option[String] = None) = Authorised.async {
    implicit authContext => implicit request => {
      addData[ResponsiblePerson](ResponsiblePerson.default(None)).map { idx =>
        Redirect {
          flow match {
            case Some(_) => controllers.responsiblepeople.routes.WhatYouNeedController.get(idx, flow)
            case _ => redirectDependingOnGuidance(displayGuidance, idx, flow)
          }
        }
      }



    }
  }

  private def redirectDependingOnGuidance(displayGuidance: Boolean, idx: Int, flow: Option[String]) = {
    displayGuidance match {
      case true => controllers.responsiblepeople.routes.WhoMustRegisterController.get(idx, flow)
      case false => controllers.responsiblepeople.routes.WhatYouNeedController.get(idx)
    }
  }
}