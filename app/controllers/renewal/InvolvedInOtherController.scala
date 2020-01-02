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

package controllers.renewal

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.businessmatching._
import models.renewal.{InvolvedInOther, InvolvedInOtherNo, InvolvedInOtherYes, Renewal}
import play.api.mvc.MessagesControllerComponents
import services.RenewalService
import utils.AuthAction
import scala.concurrent.ExecutionContext.Implicits.global
import views.html.renewal.involved_in_other

@Singleton
class InvolvedInOtherController @Inject()(val dataCacheConnector: DataCacheConnector,
                                          val authAction: AuthAction,
                                          val ds: CommonPlayDependencies,
                                          val renewalService: RenewalService,
                                          val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
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
                Ok(involved_in_other(Form2[InvolvedInOther](involvedInOther), edit, businessMatching.prefixedAlphabeticalBusinessTypes(false)))
              }) getOrElse Ok(involved_in_other(EmptyForm, edit, businessMatching.prefixedAlphabeticalBusinessTypes(false)))
            }) getOrElse Ok(involved_in_other(EmptyForm, edit, None))
        }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request => {
        Form2[InvolvedInOther](request.body) match {
          case f: InvalidForm =>
            for {
              businessMatching <- dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key)
            } yield businessMatching match {
              case Some(_) => BadRequest(involved_in_other(f, edit, businessMatching.prefixedAlphabeticalBusinessTypes(false)))
              case None => BadRequest(involved_in_other(f, edit, None))
            }
          case ValidForm(_, data) =>
            for {
              renewal <- renewalService.getRenewal(request.credId)
              _ <- renewalService.updateRenewal(request.credId, getUpdatedRenewal(renewal, data))
            } yield data match {
              case models.renewal.InvolvedInOtherYes(_) => Redirect(routes.BusinessTurnoverController.get(edit))
              case models.renewal.InvolvedInOtherNo => redirectDependingOnEdit(edit)
            }
        }
      }
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

  private def redirectDependingOnEdit(edit: Boolean) = edit match {
    case true => Redirect(routes.SummaryController.get())
    case false => Redirect(routes.AMLSTurnoverController.get(edit))
  }

}




