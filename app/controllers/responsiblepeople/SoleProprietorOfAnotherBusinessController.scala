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

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.responsiblepeople.SoleProprietorFormProvider
import models.responsiblepeople.{ResponsiblePerson, SoleProprietorOfAnotherBusiness, VATRegistered}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StatusService
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.SoleProprietorView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class SoleProprietorOfAnotherBusinessController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val statusService: StatusService,
  val cc: MessagesControllerComponents,
  formProvider: SoleProprietorFormProvider,
  view: SoleProprietorView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request =>
      getData[ResponsiblePerson](request.credId, index) map { responsiblePerson =>
        responsiblePerson.fold(NotFound(notFoundView)) { person =>
          (person.personName, person.soleProprietorOfAnotherBusiness, person.lineId, person.vatRegistered) match {
            case (Some(name), Some(soleProprietor), _, _) =>
              Ok(view(formProvider().fill(soleProprietor), edit, index, flow, name.titleName))
            case (Some(name), _, None, _)                 =>
              Ok(view(formProvider(), edit, index, flow, name.titleName))
            case (Some(_), _, _, Some(_))                 =>
              Redirect(routes.VATRegisteredController.get(index, edit, flow))
            case (Some(_), _, _, None)                    =>
              Redirect(routes.RegisteredForSelfAssessmentController.get(index, edit, flow))
            case _                                        => NotFound(notFoundView)
          }
        }
      }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request =>
      formProvider()
        .bindFromRequest()
        .fold(
          formWithErrors =>
            getData[ResponsiblePerson](request.credId, index) flatMap { rp =>
              Future.successful(BadRequest(view(formWithErrors, edit, index, flow, ControllerHelper.rpTitleName(rp))))
            },
          data =>
            {
              for {
                _ <- updateDataStrict[ResponsiblePerson](request.credId, index) { rp =>
                       rp.copy(soleProprietorOfAnotherBusiness = Some(data), vatRegistered = getVatRegData(rp, data))
                     }
              } yield
                if (data.soleProprietorOfAnotherBusiness equals true) {
                  Redirect(routes.VATRegisteredController.get(index, edit, flow))
                } else {
                  if (edit) {
                    Redirect(routes.DetailedAnswersController.get(index, flow))
                  } else {
                    Redirect(routes.RegisteredForSelfAssessmentController.get(index, edit, flow))
                  }
                }
            }.recoverWith { case _: IndexOutOfBoundsException =>
              Future.successful(NotFound(notFoundView))
            }
        )
  }

  def getVatRegData(rp: ResponsiblePerson, data: SoleProprietorOfAnotherBusiness): Option[VATRegistered] =
    if (data.soleProprietorOfAnotherBusiness) {
      rp.vatRegistered
    } else {
      None
    }
}
