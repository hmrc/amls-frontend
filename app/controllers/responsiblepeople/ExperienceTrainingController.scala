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

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.responsiblepeople.ExperienceTrainingFormProvider
import models.businessmatching.BusinessMatching
import models.responsiblepeople.ResponsiblePerson
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.businessmatching.RecoverActivitiesService
import utils.CharacterCountParser.cleanData
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.ExperienceTrainingView

import scala.concurrent.Future

class ExperienceTrainingController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val recoverActivitiesService: RecoverActivitiesService,
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: ExperienceTrainingFormProvider,
  view: ExperienceTrainingView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection
    with Logging {
  def get(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request =>
      businessMatchingData(request.credId) flatMap { bm =>
        getData[ResponsiblePerson](request.credId, index) map { responsiblePerson =>
          responsiblePerson.fold(NotFound(notFoundView)) { person =>
            val serviceOpt = getSingleService(bm)
            (person.personName, person.experienceTraining) match {
              case (Some(name), Some(training)) =>
                Ok(view(formProvider(name.titleName, serviceOpt).fill(training), bm, edit, index, flow, name.titleName))
              case (Some(name), _)              =>
                Ok(view(formProvider(name.titleName, serviceOpt), bm, edit, index, flow, name.titleName))
              case _                            =>
                NotFound(notFoundView)
            }
          }
        }
      }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] =
    authAction.async { implicit request =>
      {
        for {
          bm  <- businessMatchingData(request.credId)
          rp  <- getData[ResponsiblePerson](request.credId, index)
          name = ControllerHelper.rpTitleName(rp)
          fp   =
            formProvider(name, getSingleService(bm)).bindFromRequest(cleanData(request.body, "experienceInformation"))
        } yield fp.fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, bm, edit, index, flow, name))),
          data =>
            {
              updateDataStrict[ResponsiblePerson](request.credId, index)(rp => rp.experienceTraining(data)) map {
                _ =>
                  if (edit) {
                    Redirect(routes.DetailedAnswersController.get(index, flow))
                  } else {
                    Redirect(routes.TrainingController.get(index, edit, flow))
                  }
              }
            }.recoverWith { case _: IndexOutOfBoundsException =>
              Future.successful(NotFound(notFoundView))
            }
        )
      }.flatten
    }

  private def businessMatchingData(credId: String): Future[BusinessMatching] =
    dataCacheConnector.fetchAll(credId) map { cache =>
      // $COVERAGE-OFF$
      logger.debug(cache.toString)
      // $COVERAGE-ON$
      (for {
        c                <- cache
        businessMatching <- c.getEntry[BusinessMatching](BusinessMatching.key)
      } yield businessMatching).getOrElse(BusinessMatching())
    }

  private def getSingleService(bm: BusinessMatching): Option[String] =
    bm.prefixedAlphabeticalBusinessTypes(estateAgent = true) flatMap { types =>
      if (types.size == 1) Some(types.head) else None
    }
}
