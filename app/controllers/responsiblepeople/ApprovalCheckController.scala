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

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{Form2, _}
import javax.inject.Inject
import models.responsiblepeople.ResponsiblePerson
import play.api.mvc.MessagesControllerComponents
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.approval_check

import scala.concurrent.Future


class ApprovalCheckController @Inject()(
                                         val dataCacheConnector: DataCacheConnector,
                                         authAction: AuthAction,
                                         val ds: CommonPlayDependencies,
                                         val cc: MessagesControllerComponents,
                                         approval_check: approval_check,
                                         implicit val error: views.html.error) extends AmlsBaseController(ds, cc) with RepeatingSection {

  val FIELD_NAME = "hasAlreadyPaidApprovalCheck"
  implicit val boolWrite = utils.BooleanFormReadWrite.formWrites(FIELD_NAME)
  implicit val boolRead = utils.BooleanFormReadWrite.formRule(FIELD_NAME, "error.required.rp.approval_check")

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
    implicit request =>
      getData[ResponsiblePerson](request.credId, index) map {
        case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,alreadyPassed,_,_,_,_,_,_)) if (alreadyPassed.hasAlreadyPaidApprovalCheck.isDefined) =>
          Ok(approval_check(Form2[Boolean](alreadyPassed.hasAlreadyPaidApprovalCheck.get), edit, index, flow, personName.titleName))
        case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_)) => {
          Ok(approval_check(EmptyForm, edit, index, flow, personName.titleName))
        }
        case _ => NotFound(notFoundView)
      }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) =
    authAction.async {
      implicit request =>
        Form2[Boolean](request.body) match {
          case f: InvalidForm =>
            getData[ResponsiblePerson](request.credId, index) map { rp =>
              BadRequest(approval_check(f, edit, index, flow, ControllerHelper.rpTitleName(rp)))
            }
          case ValidForm(_, data) => {
            for {
              _ <- updateDataStrict[ResponsiblePerson](request.credId, index) { rp =>
                rp.approvalFlags(rp.approvalFlags.copy(hasAlreadyPaidApprovalCheck = Some(data)))
              }
            } yield
              Redirect(routes.DetailedAnswersController.get(index, flow))
          } recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
    }
}
