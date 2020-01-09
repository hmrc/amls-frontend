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

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import models.responsiblepeople.{ResponsiblePerson, VATRegistered}
import play.api.mvc.MessagesControllerComponents
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class VATRegisteredController @Inject () (
                                         val dataCacheConnector: DataCacheConnector,
                                         authAction: AuthAction,
                                         val ds: CommonPlayDependencies,
                                         val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) with RepeatingSection {



  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
    implicit request =>
    getData[ResponsiblePerson](request.credId, index) map {
      case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,Some(vat),_,_,_,_,_,_,_,_,_)) =>
        Ok(vat_registered(Form2[VATRegistered](vat), edit, index, flow, personName.titleName))
      case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_)) =>
        Ok(vat_registered(EmptyForm, edit, index, flow, personName.titleName))
      case _ => NotFound(notFoundView)
        }
    }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
    implicit request =>
        Form2[VATRegistered](request.body) match {
          case f: InvalidForm => getData[ResponsiblePerson](request.credId, index) map { rp =>
            BadRequest(vat_registered(f, edit, index, flow, ControllerHelper.rpTitleName(rp)))
          }
          case ValidForm(_, data) => {
            for {
              _ <- updateDataStrict[ResponsiblePerson](request.credId, index) { rp =>
                rp.vatRegistered(data)
              }
            } yield edit match {
              case true => Redirect(routes.DetailedAnswersController.get(index, flow))
              case false => Redirect(routes.RegisteredForSelfAssessmentController.get(index, edit, flow))
            }
          }.recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
    }
}