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

package controllers.responsiblepeople

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.responsiblepeople.ApprovalCheckFormProvider
import forms.{Form2, _}

import javax.inject.Inject
import models.responsiblepeople.{ApprovalFlags, ResponsiblePerson}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.ApprovalCheckView

import scala.concurrent.Future

class ApprovalCheckController @Inject()(
                                         val dataCacheConnector: DataCacheConnector,
                                         authAction: AuthAction,
                                         val ds: CommonPlayDependencies,
                                         val cc: MessagesControllerComponents,
                                         formProvider: ApprovalCheckFormProvider,
                                         view: ApprovalCheckView,
                                         implicit val error: views.html.error) extends AmlsBaseController(ds, cc) with RepeatingSection {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request =>
      getData[ResponsiblePerson](request.credId, index) map {
        case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,ApprovalFlags(_, Some(hasAlreadyPaidApprovalCheck)),_,_,_,_,_,_))  =>
          Ok(view(formProvider().fill(hasAlreadyPaidApprovalCheck), edit, index, flow, personName.titleName))
        case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_)) => {
          Ok(view(formProvider(), edit, index, flow, personName.titleName))
        }
        case _ => NotFound(notFoundView)
      }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] =
    authAction.async {
      implicit request =>
        formProvider().bindFromRequest().fold(
          formWithErrors =>
            getData[ResponsiblePerson](request.credId, index) map { rp =>
              BadRequest(view(formWithErrors, edit, index, flow, ControllerHelper.rpTitleName(rp)))
            },
          data => {
            for {
              _ <- updateDataStrict[ResponsiblePerson](request.credId, index) { rp =>
                rp.approvalFlags(rp.approvalFlags.copy(hasAlreadyPaidApprovalCheck = Some(data)))
              }
            } yield
              Redirect(routes.DetailedAnswersController.get(index, flow))
          } recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        )
    }
}
