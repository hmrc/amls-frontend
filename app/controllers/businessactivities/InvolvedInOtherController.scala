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

package controllers.businessactivities

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms._
import models.businessactivities.{BusinessActivities, _}
import models.businessmatching._
import play.api.i18n.Messages
import services.StatusService
import utils.{AuthAction, ControllerHelper}
import views.html.businessactivities._

import scala.concurrent.Future

class InvolvedInOtherController @Inject() ( val dataCacheConnector: DataCacheConnector,
                                            implicit val statusService: StatusService,
                                            val authAction: AuthAction,
                                            val ds: CommonPlayDependencies)extends AmlsBaseController(ds) {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetchAll(request.credId) map {
        optionalCache =>
          (for {
            cache <- optionalCache
            businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
          } yield {
            (for {
              businessActivities <- cache.getEntry[BusinessActivities](BusinessActivities.key)
              involvedInOther <- businessActivities.involvedInOther
            } yield Ok(involved_in_other_name(Form2[InvolvedInOther](involvedInOther), edit, businessMatching.prefixedAlphabeticalBusinessTypes(false))))
              .getOrElse(Ok(involved_in_other_name(EmptyForm, edit, businessMatching.prefixedAlphabeticalBusinessTypes(false))))
          }) getOrElse Ok(involved_in_other_name(EmptyForm, edit, None))
      }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request => {
      Form2[InvolvedInOther](request.body) match {
        case f: InvalidForm =>
          for {
            businessMatching <- dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key)
          } yield businessMatching match {
            case Some(x) => BadRequest(involved_in_other_name(f, edit, businessMatching.prefixedAlphabeticalBusinessTypes(false)))
            case None => BadRequest(involved_in_other_name(f, edit, None))
          }
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](request.credId, BusinessActivities.key, getUpdatedBA(businessActivities, data))

          } yield redirectDependingOnResponse(data, edit)
      }
    }
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
      case true => Redirect(routes.SummaryController.get())
    }
  }
}
