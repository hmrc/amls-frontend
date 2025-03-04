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
import controllers.{AmlsBaseController, CommonPlayDependencies}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.responsiblepeople.YourResponsiblePeopleService
import utils.AuthAction
import views.html.responsiblepeople.YourResponsiblePeopleView

class YourResponsiblePeopleController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  yourResponsiblePeopleService: YourResponsiblePeopleService,
  view: YourResponsiblePeopleView
) extends AmlsBaseController(ds, cc) {

  def get(): Action[AnyContent] = authAction.async { implicit request =>
    yourResponsiblePeopleService.completeAndIncompleteRP(request.credId) map {
      case Some((completeRP, incompleteRP)) => Ok(view(completeRP, incompleteRP))
      case None                             => Redirect(controllers.routes.RegistrationProgressController.get())
    }
  }
}
