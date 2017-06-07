/*
 * Copyright 2017 HM Revenue & Customs
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

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.responsiblepeople.ResponsiblePeople
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection

import scala.concurrent.Future

trait ResponsiblePeopleAddController extends BaseController with RepeatingSection {
  def get(displayGuidance: Boolean = true, fromDeclaration: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      addData[ResponsiblePeople](ResponsiblePeople.default(None)).map {idx =>
        Redirect {
          displayGuidance match {
            case true => controllers.responsiblepeople.routes.WhoMustRegisterController.get(idx, fromDeclaration)
            case false => controllers.responsiblepeople.routes.PersonNameController.get(idx)
          }
        }
      }
    }
  }
}

object ResponsiblePeopleAddController extends ResponsiblePeopleAddController {
  // $COVERAGE-OFF$
  override def dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected def authConnector: AuthConnector = AMLSAuthConnector
}
