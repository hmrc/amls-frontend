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

package controllers.businessactivities

import com.google.inject.Inject
import config.AmlsErrorHandler
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.businessactivities.InvolvedInOtherFormProvider
import models.businessactivities.{BusinessActivities, _}
import models.businessmatching._
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StatusService
import services.businessmatching.RecoverActivitiesService
import utils.AuthAction
import utils.CharacterCountParser.cleanData
import views.html.businessactivities.InvolvedInOtherNameView
import scala.concurrent.{ExecutionContext, Future}

class InvolvedInOtherController @Inject()(val dataCacheConnector: DataCacheConnector,
                                          val recoverActivitiesService: RecoverActivitiesService,
                                          implicit val statusService: StatusService,
                                          val authAction: AuthAction,
                                          val ds: CommonPlayDependencies,
                                          val cc: MessagesControllerComponents,
                                          formProvider: InvolvedInOtherFormProvider,
                                          error: AmlsErrorHandler,
                                          view: InvolvedInOtherNameView)(implicit ec: ExecutionContext) extends AmlsBaseController(ds, cc) with Logging{

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async {
   implicit request =>
    dataCacheConnector.fetchAll(request.credId).flatMap {
      case Some(cache) =>
        val businessMatchingOpt = cache.getEntry[BusinessMatching](BusinessMatching.key)
        val businessActivitiesOpt = cache.getEntry[BusinessActivities](BusinessActivities.key)

        businessMatchingOpt match {
          case Some(businessMatching) =>
            val form = businessActivitiesOpt.flatMap(_.involvedInOther) match {
              case Some(involvedInOther) => formProvider().fill(involvedInOther)
              case None => formProvider()
            }
            Future.successful(Ok(view(form, edit, businessMatching.prefixedAlphabeticalBusinessTypes(false), formProvider.length)))

          case None =>
            Future.successful(Ok(view(formProvider(), edit, None, formProvider.length)))
        }

      case None =>
        Future.successful(Ok(view(formProvider(), edit, None, formProvider.length)))
    } recoverWith {
      case _: NoSuchElementException =>
        logger.warn("[InvolvedInOtherController][get] - Business activities list was empty, attempting to recover")
        recoverActivitiesService.recover(request).flatMap {
          case true => Future.successful(Redirect(routes.InvolvedInOtherController.get()))
          case false =>
            logger.warn("[InvolvedInOtherController][get] - Unable to determine business types")
            error.internalServerErrorTemplate.map(template => InternalServerError(template))
        }
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request =>
      formProvider().bindFromRequest(cleanData(request.body, "details")).fold(
        formWithErrors => {
          dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key).map {
            case Some(businessMatching) =>
              BadRequest(view(formWithErrors, edit, businessMatching.prefixedAlphabeticalBusinessTypes(false), formProvider.length))
            case None =>
              BadRequest(view(formWithErrors, edit, None, formProvider.length))
          }
        },
      data => {
        for {
          businessActivities <- dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key)
          _ <- dataCacheConnector.save[BusinessActivities](request.credId, BusinessActivities.key, getUpdatedBA(businessActivities, data))
        } yield redirectDependingOnResponse(data, edit)
      }
    )
  }

  private def getUpdatedBA(businessActivities: Option[BusinessActivities], data: InvolvedInOther): BusinessActivities = {
    (businessActivities, data) match {
      case (Some(ba), InvolvedInOtherYes(_)) => ba.copy(involvedInOther = Some(data))
      case (Some(ba), InvolvedInOtherNo) => ba.copy(involvedInOther = Some(data), expectedBusinessTurnover = None)
      case (_, _) => BusinessActivities(involvedInOther = Some(data))
    }
  }

  private def redirectDependingOnResponse(data: InvolvedInOther, edit: Boolean) = data match {
    case InvolvedInOtherYes(_) => Redirect(routes.ExpectedBusinessTurnoverController.get(edit))
    case InvolvedInOtherNo => edit match {
      case false => Redirect(routes.ExpectedAMLSTurnoverController.get(edit))
      case true => Redirect(routes.SummaryController.get)
    }
  }
}
