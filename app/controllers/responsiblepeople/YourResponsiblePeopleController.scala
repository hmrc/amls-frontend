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

package controllers.responsiblepeople

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import models.responsiblepeople.ResponsiblePerson
import play.api.mvc.MessagesControllerComponents
import utils.{AuthAction, RepeatingSection}
import views.html.responsiblepeople.your_responsible_people
import scala.concurrent.ExecutionContext.Implicits.global

class YourResponsiblePeopleController @Inject () (
                                                 val dataCacheConnector: DataCacheConnector,
                                                 authAction: AuthAction,
                                                 val ds: CommonPlayDependencies,
                                                 val cc: MessagesControllerComponents,
                                                 your_responsible_people: your_responsible_people) extends AmlsBaseController(ds, cc) with RepeatingSection {

  def get() =
      authAction.async {
        implicit request =>
          dataCacheConnector.fetch[Seq[ResponsiblePerson]](request.credId, ResponsiblePerson.key) map {
            case Some(data) => {
              val (completeRP, incompleteRP) = ResponsiblePerson.filterWithIndex(data)
                .partition(_._1.isComplete)

              Ok(your_responsible_people(completeRP, incompleteRP))
            }
            case _ => Redirect(controllers.routes.RegistrationProgressController.get())
          }
      }
}
