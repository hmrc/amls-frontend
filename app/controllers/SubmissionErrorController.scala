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

package controllers

import play.api.mvc._
import utils.AuthAction
import views.html.submission._

import javax.inject.Inject

class SubmissionErrorController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  duplicateEnrolmentView: DuplicateEnrolmentView,
  duplicateSubmissionView: DuplicateSubmissionView,
  wrongCredentialTypeView: WrongCredentialTypeView,
  badRequestView: BadRequestView
) extends AmlsBaseController(ds, cc) {

  def duplicateEnrolment(): Action[AnyContent] = authAction { implicit request =>
    Ok(duplicateEnrolmentView(appConfig.contactFrontendReportUrl))
  }

  def duplicateSubmission(): Action[AnyContent] = authAction { implicit request =>
    Ok(duplicateSubmissionView(appConfig.contactFrontendReportUrl))
  }

  def wrongCredentialType(): Action[AnyContent] = authAction { implicit request =>
    Ok(wrongCredentialTypeView(appConfig.contactFrontendReportUrl))
  }

  def badRequest(): Action[AnyContent] = authAction { implicit request =>
    Ok(badRequestView(appConfig.contactFrontendReportUrl))
  }
}
