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

package controllers.declaration

import javax.inject.{Inject, Singleton}
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionReadyForReview}
import play.api.mvc.MessagesControllerComponents
import services.StatusService
import utils.AuthAction
import views.html.declaration.register_responsible_person

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class RegisterResponsiblePersonController @Inject()(val dataCacheConnector: DataCacheConnector,
                                                    authAction: AuthAction,
                                                    val ds: CommonPlayDependencies,
                                                    val statusService: StatusService,
                                                    val cc: MessagesControllerComponents,
                                                    register_responsible_person: register_responsible_person) extends AmlsBaseController(ds, cc) {

  def get() = authAction.async {
    implicit request => {

      statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId) map {
        case ReadyForRenewal(_) |
             SubmissionDecisionApproved |
             SubmissionReadyForReview => Ok(register_responsible_person("submit.amendment.application"))
        case _ => Ok(register_responsible_person("submit.registration"))
      }
    }
  }
}
