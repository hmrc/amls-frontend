/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.businessmatching.updateservice.remove

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Write}
import models.businessmatching.{BusinessActivities, BusinessActivity}
import models.flowmanagement.{RemoveBusinessTypeFlowModel, WhatBusinessTypesToRemovePageId}
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.MappingUtils.Implicits._
import utils.TraversableValidators.minLengthR
import views.html.businessmatching.updateservice.remove.remove_activities

import scala.concurrent.Future

@Singleton
class RemoveBusinessTypesController @Inject()(
                                          val authConnector: AuthConnector,
                                          val dataCacheConnector: DataCacheConnector,
                                          val businessMatchingService: BusinessMatchingService,
                                          val router: Router[RemoveBusinessTypeFlowModel]

) extends BaseController {

  implicit def businessActivityRule = From[UrlFormEncoded] { __ =>
    (__ \ "businessActivities").read(minLengthR[Set[BusinessActivity]](1).withMessage("error.required.bm.remove.service"))
  }

  implicit def activitySetWrites(implicit w: Write[BusinessActivity, String]) = Write[Set[BusinessActivity], UrlFormEncoded] { activities =>
    Map("businessActivities[]" ->  activities.toSeq.map { a => BusinessActivities.getValue(a) })
  }

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        (for {
          model <- OptionT(dataCacheConnector.fetch[RemoveBusinessTypeFlowModel](RemoveBusinessTypeFlowModel.key))
            .orElse(OptionT.some(RemoveBusinessTypeFlowModel()))
          (_, values) <- getFormData
        } yield {
          val form = model.activitiesToRemove.fold[Form2[Set[BusinessActivity]]](EmptyForm)(a => Form2(a))
          Ok(remove_activities(form, edit, values))
        }) getOrElse InternalServerError("Get: Unable to show remove Activities page. Failed to retrieve data")
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[Set[BusinessActivity]](request.body) match {
          case f: InvalidForm => getFormData map {
            case (_, values) =>
              BadRequest(remove_activities(f, edit, values))
          } getOrElse InternalServerError("Post: Invalid form on Remove Activities page")

          case ValidForm(_, data) =>
            dataCacheConnector.update[RemoveBusinessTypeFlowModel](RemoveBusinessTypeFlowModel.key) {
              case Some(model) =>
                model.copy(activitiesToRemove = Some(data))
              case _ => RemoveBusinessTypeFlowModel(Some(data))
            } flatMap {
              case Some(model) => router.getRoute(WhatBusinessTypesToRemovePageId, model, edit)
              case _ => Future.successful(InternalServerError("Post: Cannot retrieve data: RemoveActivitiesController"))
            }
        }
  }

  private def getFormData(implicit ac: AuthContext, hc: HeaderCarrier) = for {
    model <- businessMatchingService.getModel
    activities <- OptionT.fromOption[Future](model.activities) map {
      _.businessActivities
    }
  } yield {
    val existingActivityNames = activities.toSeq.sortBy(_.getMessage()) map {
      _.getMessage()
    }

    val activityValues = activities.toSeq.sortBy(_.getMessage()) map BusinessActivities.getValue

    (existingActivityNames, activityValues)
  }
}
