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

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.DefaultBaseController
import forms._
import models.responsiblepeople.{ResponsiblePerson, SaRegistered}
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople._

import scala.concurrent.Future

class RegisteredForSelfAssessmentController @Inject () (
                                                       val dataCacheConnector: DataCacheConnector,
                                                       authAction: AuthAction
                                                       ) extends RepeatingSection with DefaultBaseController {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) =
    authAction.async {
      implicit request =>
        getData[ResponsiblePerson](request.credId, index) map {
          case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_, Some(person),_,_,_,_,_,_,_,_,_,_))
          => Ok(registered_for_self_assessment(Form2[SaRegistered](person), edit, index, flow, personName.titleName))
          case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_))
          => Ok(registered_for_self_assessment(EmptyForm, edit, index, flow, personName.titleName))
          case _
          => NotFound(notFoundView)
        }
    }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) =
    authAction.async {
      implicit request =>
        Form2[SaRegistered](request.body) match {
          case f: InvalidForm =>
            getData[ResponsiblePerson](request.credId, index) map { rp =>
              BadRequest(registered_for_self_assessment(f, edit, index, flow, ControllerHelper.rpTitleName(rp)))
            }
          case ValidForm(_, data) => {
            for {
              _ <- updateDataStrict[ResponsiblePerson](request.credId, index) { rp =>
                rp.saRegistered(data)
              }
            } yield {
              edit match {
                case false => Redirect(routes.ExperienceTrainingController.get(index, edit, flow))
                case true => Redirect(routes.DetailedAnswersController.get(index, flow))
              }
            }
          }.recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
    }
}