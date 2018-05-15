/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.responsiblepeople.{PreviousName, ResponsiblePerson}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.legal_name

import scala.concurrent.Future

@Singleton
class LegalNameController @Inject()(val dataCacheConnector: DataCacheConnector,
                                    val authConnector: AuthConnector) extends RepeatingSection with BaseController {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = Authorised.async {
    implicit authContext =>
      implicit request =>
        getData[ResponsiblePerson](index) map {
          case Some(ResponsiblePerson(Some(personName), Some(previous), _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _))
          => Ok(legal_name(Form2[PreviousName](previous), edit, index, flow, personName.titleName))
          case Some(ResponsiblePerson(Some(personName), _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _))
          => Ok(legal_name(EmptyForm, edit, index, flow, personName.titleName))
          case _
          => NotFound(notFoundView)
        }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) = Authorised.async {
    implicit authContext =>
      implicit request => {
        Form2[PreviousName](request.body) match {
          case f: InvalidForm =>
            getData[ResponsiblePerson](index) map { rp =>
              BadRequest(views.html.responsiblepeople.legal_name(f, edit, index, flow, ControllerHelper.rpTitleName(rp)))
            }
          case ValidForm(_, data) => {
            for {
              _ <- {
                data.hasPreviousName match {
                  case Some(true) => updateDataStrict[ResponsiblePerson](index) { rp =>
                    rp.legalName(data)
                  }
                  case Some(false) => updateDataStrict[ResponsiblePerson](index) { rp =>
                    rp.legalName(PreviousName(Some(false), None, None, None)).copy(legalNameChangeDate = None)
                  }
                }
              }
            } yield edit match {
              case true if data.hasPreviousName.contains(true) => Redirect(routes.LegalNameChangeDateController.get(index, edit, flow))
              case true => Redirect(routes.DetailedAnswersController.get(index, edit, flow))
              case false if data.hasPreviousName.contains(true) =>
                Redirect(routes.LegalNameChangeDateController.get(index, edit, flow))
              case _ => Redirect(routes.KnownByController.get(index, edit, flow))
            }
          }.recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
      }
  }

}
