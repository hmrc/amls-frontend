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

package controllers.renewal

import config.AmlsErrorHandler
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.renewal.InvolvedInOtherFormProvider

import javax.inject.{Inject, Singleton}
import models.businessmatching._
import models.renewal.{InvolvedInOther, InvolvedInOtherNo, InvolvedInOtherYes, Renewal}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.RenewalService
import services.businessmatching.RecoverActivitiesService
import utils.AuthAction
import utils.CharacterCountParser.cleanData
import views.html.renewal.InvolvedInOtherView

@Singleton
class InvolvedInOtherController @Inject()(val dataCacheConnector: DataCacheConnector,
                                          val recoverActivitiesService: RecoverActivitiesService,
                                          val authAction: AuthAction,
                                          val ds: CommonPlayDependencies,
                                          val renewalService: RenewalService,
                                          val cc: MessagesControllerComponents,
                                          formProvider: InvolvedInOtherFormProvider,
                                          error: AmlsErrorHandler,
                                          view: InvolvedInOtherView) extends AmlsBaseController(ds, cc) with Logging {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request =>
        dataCacheConnector.fetchAll(request.credId).map {
          optionalCache =>
            (for {
              cache <- optionalCache
              businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
            } yield {
              (for {
                renewal <- cache.getEntry[Renewal](Renewal.key)
                involvedInOther <- renewal.involvedInOtherActivities
              } yield {
                Ok(view(formProvider().fill(involvedInOther), edit, businessMatching.prefixedAlphabeticalBusinessTypes(false)))
              }) getOrElse Ok(view(formProvider(), edit, businessMatching.prefixedAlphabeticalBusinessTypes(false)))
            }) getOrElse Ok(view(formProvider(), edit, None))
        } recoverWith {
          case _: NoSuchElementException =>
            logger.warn("[InvolvedInOtherController][get] - Business activities list was empty, attempting to recover")
            recoverActivitiesService.recover(request).map {
              case true => Redirect(routes.InvolvedInOtherController.get())
              case false =>
                logger.warn("[InvolvedInOtherController][get] - Unable to determine business types")
                InternalServerError(error.internalServerErrorTemplate)
            }
        }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request =>
      formProvider().bindFromRequest(cleanData(request.body, "details")).fold(
        formWithErrors =>
          for {
            businessMatching <- dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key)
          } yield businessMatching match {
            case Some(_) => BadRequest(view(formWithErrors, edit, businessMatching.prefixedAlphabeticalBusinessTypes(false)))
            case None => BadRequest(view(formWithErrors, edit, None))
          },
        data =>
          for {
            renewal <- renewalService.getRenewal(request.credId)
            _ <- renewalService.updateRenewal(request.credId, getUpdatedRenewal(renewal, data))
          } yield data match {
            case models.renewal.InvolvedInOtherYes(_) => Redirect(routes.BusinessTurnoverController.get(edit))
            case models.renewal.InvolvedInOtherNo => redirectDependingOnEdit(edit)
          }
    )
  }

  private def getUpdatedRenewal(renewal: Option[Renewal], data: InvolvedInOther): Renewal = {
    (renewal, data) match {
      case (Some(_), InvolvedInOtherYes(_)) => {
        renewal.involvedInOtherActivities(data)
      }
      case (Some(_), InvolvedInOtherNo) => {
        renewal.involvedInOtherActivities(data).resetBusinessTurnover
      }
      case (None, _) => {
        Renewal(involvedInOtherActivities = Some(data), hasChanged = true)
      }
    }
  }

  private def redirectDependingOnEdit(edit: Boolean) = if (edit) {
    Redirect(routes.SummaryController.get)
  } else {
    Redirect(routes.AMLSTurnoverController.get(edit))
  }

}




