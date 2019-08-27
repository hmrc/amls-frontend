/*
 * Copyright 2019 HM Revenue & Customs
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
import models.businessmatching.BusinessMatching
import models.responsiblepeople.{ExperienceTraining, ResponsiblePerson}
import play.api.Logger
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.experience_training

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class ExperienceTrainingController @Inject () (
                                              val dataCacheConnector: DataCacheConnector,
                                              authAction: AuthAction
                                              ) extends RepeatingSection with DefaultBaseController {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
      implicit request =>
        businessMatchingData(request.credId) flatMap {
          bm =>
            getData[ResponsiblePerson](request.credId, index) map {
              case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_, Some(experienceTraining),_,_,_,_,_,_,_,_))
              => Ok(experience_training(Form2[ExperienceTraining](experienceTraining), bm.alphabeticalBusinessActivitiesLowerCase, edit, index, flow, personName.titleName))
              case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_))
              => Ok(experience_training(EmptyForm, bm.alphabeticalBusinessTypes, edit, index, flow, personName.titleName))
              case _
              => NotFound(notFoundView)
            }
        }
    }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) =
    authAction.async {
      implicit request => {
        businessMatchingData(request.credId) flatMap {
          bm =>
            Form2[ExperienceTraining](request.body) match {
              case f: InvalidForm =>
                getData[ResponsiblePerson](request.credId, index) map { rp =>
                  BadRequest(views.html.responsiblepeople.experience_training(f, bm.alphabeticalBusinessActivitiesLowerCase, edit, index, flow, ControllerHelper.rpTitleName(rp)))
                }
              case ValidForm(_, data) => {
                for {
                  result <- updateDataStrict[ResponsiblePerson](request.credId, index) { rp => rp.experienceTraining(data) }
                } yield if (edit) {
                  Redirect(routes.DetailedAnswersController.get(index, flow))
                } else {
                  Redirect(routes.TrainingController.get(index, edit, flow))
                }
              }.recoverWith {
                case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
              }
            }
        }
      }
    }

  private def businessMatchingData(credId: String)(implicit hc: HeaderCarrier): Future[BusinessMatching] = {
    dataCacheConnector.fetchAll(credId) map {
      cache =>
        Logger.debug(cache.toString)
        (for {
          c <- cache
          businessMatching <- c.getEntry[BusinessMatching](BusinessMatching.key)
        } yield businessMatching).getOrElse(BusinessMatching())
    }
  }
}