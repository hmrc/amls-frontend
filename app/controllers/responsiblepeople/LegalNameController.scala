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
import forms.responsiblepeople.LegalNameFormProvider
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}

import javax.inject.{Inject, Singleton}
import models.responsiblepeople.{PreviousName, ResponsiblePerson}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.LegalNameView

import scala.concurrent.Future


@Singleton
class LegalNameController @Inject()(val dataCacheConnector: DataCacheConnector,
                                    authAction: AuthAction,
                                    val ds: CommonPlayDependencies,
                                    val cc: MessagesControllerComponents,
                                    formProvider: LegalNameFormProvider,
                                    view: LegalNameView,
                                    implicit val error: views.html.ErrorView) extends AmlsBaseController(ds, cc) with RepeatingSection {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request =>
      getData[ResponsiblePerson](request.credId, index) map { responsiblePerson =>
        responsiblePerson.fold(NotFound(notFoundView)) { person =>
          (person.personName, person.legalName) match {
            case (Some(name), Some(previousName)) => Ok(view(formProvider().fill(previousName), edit, index, flow, name.titleName))
            case (Some(name), _) => Ok(view(formProvider(), edit, index, flow, name.titleName))
            case _ => NotFound(notFoundView)
          }
        }
      }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request => {
      formProvider().bindFromRequest().fold(
        formWithError =>
          getData[ResponsiblePerson](request.credId, index) map { rp =>
            BadRequest(view(formWithError, edit, index, flow, ControllerHelper.rpTitleName(rp)))
          },
        data => {
          for {
            _ <- {
              data.hasPreviousName match {
                case Some(true) => updateDataStrict[ResponsiblePerson](request.credId, index) { rp =>
                  rp.legalName(data)
                }
                case Some(false) => updateDataStrict[ResponsiblePerson](request.credId, index) { rp =>
                  rp.legalName(PreviousName(Some(false), None, None, None)).copy(legalNameChangeDate = None)
                }
                case None => throw new Exception("An UnknownException has occurred: LegalNameController")
              }
            }
          } yield edit match {
            case true if data.hasPreviousName.contains(true) => Redirect(routes.LegalNameInputController.get(index, edit, flow))
            case true => Redirect(routes.DetailedAnswersController.get(index, flow))
            case false if data.hasPreviousName.contains(true) => Redirect(routes.LegalNameInputController.get(index, edit, flow))
            case _ => Redirect(routes.KnownByController.get(index, edit, flow))
          }
        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      )
    }
  }
}
