/*
 * Copyright 2017 HM Revenue & Customs
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
import models.responsiblepeople.{PreviousName, ResponsiblePeople}
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
        getData[ResponsiblePeople](index) map {
          case Some(ResponsiblePeople(Some(personName),Some(previous),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_))
          => Ok(legal_name(Form2[PreviousName](previous), edit, index, flow, personName.titleName ))
          case Some(ResponsiblePeople(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_))
          => Ok(legal_name(EmptyForm, edit, index, flow, personName.titleName))
          case _
          => NotFound(notFoundView)
        }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) = Authorised.async {
    implicit authContext =>
      implicit request => {
        request.body.asFormUrlEncoded map { form =>
          form.get("hasPreviousName") map { list => list.head == "true" }
        }

        Form2[PreviousName](request.body) match {
          case f: InvalidForm =>
            getData[ResponsiblePeople](index) map { rp =>
              BadRequest(views.html.responsiblepeople.legal_name(f, edit, index, flow, ControllerHelper.rpTitleName(rp)))
            }
          case ValidForm(_, data) => {
            for {
              _ <- updateDataStrict[ResponsiblePeople](index) { rp =>
                rp.legalName(data)
              }
            } yield edit match {
              case true => Redirect(routes.DetailedAnswersController.get(index, edit, flow))
              case false if data.firstName.isDefined || data.middleName.isDefined || data.lastName.isDefined =>
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
