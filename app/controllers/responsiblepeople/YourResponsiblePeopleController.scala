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
import views.html.responsiblepeople.your_responsible_people

class YourResponsiblePeopleController @Inject () (
                                                 val dataCacheConnector: DataCacheConnector,
                                                 val authConnector: AuthConnector
                                                 ) extends RepeatingSection with BaseController {

  def get() =
      Authorised.async {
        implicit authContext => implicit request =>
          dataCacheConnector.fetch[Seq[ResponsiblePerson]](ResponsiblePerson.key) map {
            case Some(data) => {
              val (completeRP, incompleteRP) = ResponsiblePerson.filterWithIndex(data)
                .partition(_._1.isComplete)

              Ok(your_responsible_people(completeRP, incompleteRP))
            }
            case _ => Redirect(controllers.routes.RegistrationProgressController.get())
          }
      }
}
