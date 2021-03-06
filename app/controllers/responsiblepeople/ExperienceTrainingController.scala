/*
 * Copyright 2021 HM Revenue & Customs
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
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms._
import models.businessmatching.BusinessMatching
import models.responsiblepeople.{ExperienceTraining, ResponsiblePerson}
import play.api.Logger
import play.api.mvc.MessagesControllerComponents
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.experience_training
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier


class ExperienceTrainingController @Inject () (val dataCacheConnector: DataCacheConnector,
                                               authAction: AuthAction,
                                               val ds: CommonPlayDependencies,
                                               val cc: MessagesControllerComponents,
                                               experience_training: experience_training,
                                               implicit val error: views.html.error) extends AmlsBaseController(ds, cc) with RepeatingSection {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
      implicit request =>
        businessMatchingData(request.credId) flatMap {
          bm =>
            getData[ResponsiblePerson](request.credId, index) map {
              case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_, Some(experienceTraining),_,_,_,_,_,_,_,_))
              => Ok(experience_training(Form2[ExperienceTraining](experienceTraining), bm, edit, index, flow, personName.titleName))
              case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_))
              => Ok(experience_training(EmptyForm, bm, edit, index, flow, personName.titleName))
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
                  BadRequest(experience_training(f, bm, edit, index, flow, ControllerHelper.rpTitleName(rp)))
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
        // $COVERAGE-OFF$
        Logger.debug(cache.toString)
        // $COVERAGE-ON$
        (for {
          c <- cache
          businessMatching <- c.getEntry[BusinessMatching](BusinessMatching.key)
        } yield businessMatching).getOrElse(BusinessMatching())
    }
  }
}