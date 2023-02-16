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

package controllers.businessmatching

import controllers.{AmlsBaseController, CommonPlayDependencies}
import javax.inject.Inject
import models.status.{NotCompleted, SubmissionReady}
import play.api.mvc.MessagesControllerComponents
import services.StatusService
import utils.AuthAction
import views.html.businessmatching.{cannot_add_services, cannot_continue_with_the_application}

class NoPsrController @Inject()(val authAction: AuthAction,
                                val ds: CommonPlayDependencies,
                                statusService: StatusService,
                                val cc: MessagesControllerComponents,
                                cannot_add_services: cannot_add_services,
                                cannot_continue_with_the_application: cannot_continue_with_the_application) extends AmlsBaseController(ds, cc) {

  def get = authAction.async {
    implicit request =>
      statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId) map {
        case NotCompleted | SubmissionReady => Ok(cannot_continue_with_the_application())
        case _ => Ok(cannot_add_services())
      }
  }
}
