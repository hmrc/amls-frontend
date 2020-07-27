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

package controllers.businessmatching.updateservice.remove

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import controllers.businessmatching.updateservice.RemoveBusinessTypeHelper
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.businessmatching.{BusinessActivities, BusinessActivity}
import models.flowmanagement.{RemoveBusinessTypeFlowModel, WhatBusinessTypesToRemovePageId}
import play.api.mvc.MessagesControllerComponents
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import uk.gov.hmrc.http.HeaderCarrier
import utils.AuthAction
import scala.concurrent.ExecutionContext.Implicits.global
import views.html.businessmatching.updateservice.remove.remove_activities

import scala.concurrent.Future

@Singleton
class RemoveBusinessTypesController @Inject()(
                                               authAction: AuthAction,
                                               val ds: CommonPlayDependencies,
                                               val dataCacheConnector: DataCacheConnector,
                                               val businessMatchingService: BusinessMatchingService,
                                               val removeBusinessTypeHelper: RemoveBusinessTypeHelper,
                                               val router: Router[RemoveBusinessTypeFlowModel],
                                               val cc: MessagesControllerComponents,
                                               remove_activities: remove_activities) extends AmlsBaseController(ds, cc) {

  import models.businessmatching.BusinessActivities._

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        (for {
          model <- OptionT(dataCacheConnector.fetch[RemoveBusinessTypeFlowModel](request.credId, RemoveBusinessTypeFlowModel.key))
            .orElse(OptionT.some(RemoveBusinessTypeFlowModel()))
          (_, values) <- getFormData(request.credId)
        } yield {
          val form = model.activitiesToRemove.fold[Form2[Set[BusinessActivity]]](EmptyForm)(a => Form2(a))
          Ok(remove_activities(form, edit, values))
        }) getOrElse InternalServerError("Get: Unable to show remove Activities page. Failed to retrieve data")
  }

  def post(edit: Boolean = false) = authAction.async {
      implicit request =>

        val errorMessage = (for {
          submittedActivities <- businessMatchingService.getSubmittedBusinessActivities(request.credId)
        } yield {if(submittedActivities.size > 2) {
          "error.required.bm.remove.service.multiple"
        } else {
          "error.required.bm.remove.service"
        }}).value

        val formData = (for {
          data <- getFormData(request.credId)
        } yield data).value

        errorMessage.flatMap(msg =>
              formData flatMap {
                case Some((names, values)) =>
                  Form2[Set[BusinessActivity]](request.body)(combinedReader(names.size, msg.getOrElse(""))) match {
                    case f: InvalidForm => getFormData(request.credId) map {
                      case (_, values) =>
                        BadRequest(remove_activities(f, edit, values))
                    } getOrElse InternalServerError("Post: Invalid form on Remove Activities page")

                    case ValidForm(_, data) =>
                      (for {
                        model <- OptionT(dataCacheConnector.fetch[RemoveBusinessTypeFlowModel](request.credId, RemoveBusinessTypeFlowModel.key)) orElse OptionT.some(RemoveBusinessTypeFlowModel())
                        dateApplicable <- removeBusinessTypeHelper.dateOfChangeApplicable(request.credId, data)
                        servicesChanged <- OptionT.some[Future, Boolean](model.activitiesToRemove.getOrElse(Set.empty) != data)
                        newModel <- OptionT.some[Future, RemoveBusinessTypeFlowModel](
                          model.copy(activitiesToRemove = Some(data), dateOfChange = if (!dateApplicable || servicesChanged) None else model.dateOfChange)
                        )
                        _ <- OptionT.liftF(dataCacheConnector.save(request.credId, RemoveBusinessTypeFlowModel.key, newModel))

                        route <- OptionT.liftF(router.getRoute(request.credId, WhatBusinessTypesToRemovePageId, newModel, edit))
                      } yield route) getOrElse InternalServerError("Post: Cannot retrieve data: RemoveActivitiesController")

                  }
              }

        )

      }




  private def getFormData(credId: String)(implicit hc: HeaderCarrier) = for {
    model <- businessMatchingService.getModel(credId)
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
