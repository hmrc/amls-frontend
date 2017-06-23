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

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.responsiblepeople.{ResponsiblePeople, SaRegistered}
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople._

import scala.concurrent.Future

trait RegisteredForSelfAssessmentController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false, fromYourAnswers: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>
        getData[ResponsiblePeople](index) map {
          case Some(ResponsiblePeople(Some(personName),_,_,_,_,_,_,_, Some(person),_,_,_,_,_,_,_,_,_))
          => Ok(registered_for_self_assessment(Form2[SaRegistered](person), edit, index, fromYourAnswers, personName.titleName))
          case Some(ResponsiblePeople(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_))
          => Ok(registered_for_self_assessment(EmptyForm, edit, index, fromYourAnswers, personName.titleName))
          case _
          => NotFound(notFoundView)
        }
    }

  def post(index: Int, edit: Boolean = false, fromYourAnswers: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>
        Form2[SaRegistered](request.body) match {
          case f: InvalidForm =>
            getData[ResponsiblePeople](index) map {rp =>
              BadRequest(registered_for_self_assessment(f, edit, index, fromYourAnswers, ControllerHelper.rpTitleName(rp)))
            }
          case ValidForm(_, data) => {
            for {
              _ <- updateDataStrict[ResponsiblePeople](index) { rp =>
                rp.saRegistered(data)
              }
            } yield {
              edit match {
                case false => Redirect(routes.ExperienceTrainingController.get(index, edit, fromYourAnswers))
                case true => Redirect(routes.DetailedAnswersController.get(index))
              }
            }
          }.recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
    }
}

object RegisteredForSelfAssessmentController extends RegisteredForSelfAssessmentController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector

  override def dataCacheConnector = DataCacheConnector
}

