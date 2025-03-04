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

package controllers.businessmatching.updateservice.remove

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.businessmatching.updateservice.RemoveBusinessTypeHelper
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.businessmatching.RemoveBusinessActivitiesFormProvider
import models.businessmatching.{BusinessActivities, BusinessActivity}
import models.flowmanagement.{RemoveBusinessTypeFlowModel, WhatBusinessTypesToRemovePageId}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import utils.{AuthAction, AuthorisedRequest}
import views.html.businessmatching.updateservice.remove.RemoveActivitiesView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class RemoveBusinessTypesController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val dataCacheConnector: DataCacheConnector,
  val businessMatchingService: BusinessMatchingService,
  val removeBusinessTypeHelper: RemoveBusinessTypeHelper,
  val router: Router[RemoveBusinessTypeFlowModel],
  val cc: MessagesControllerComponents,
  formProvider: RemoveBusinessActivitiesFormProvider,
  view: RemoveActivitiesView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    (for {
      model             <-
        OptionT(dataCacheConnector.fetch[RemoveBusinessTypeFlowModel](request.credId, RemoveBusinessTypeFlowModel.key))
          .orElse(OptionT.some(RemoveBusinessTypeFlowModel()))
      (_, values)       <- getFormData(request.credId)
      valuesAsActivities = values.map(BusinessActivities.getBusinessActivity)
      form               = formProvider(valuesAsActivities.length)
    } yield {
      val formForView = model.activitiesToRemove.fold(form) { x =>
        form.fill(x.toSeq)
      }
      Ok(view(formForView, edit, valuesAsActivities))
    }) getOrElse InternalServerError("Get: Unable to show remove Activities page. Failed to retrieve data")
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    (for {
      data <- getFormData(request.credId)
      form  = formProvider(data._2.size)
    } yield form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(
              view(
                formWithErrors,
                edit,
                data._2.map(BusinessActivities.getBusinessActivity)
              )
            )
          ),
        formValue => saveAndRedirect(formValue, edit)
      )).value flatMap {
      case Some(value) => value
      case None        => Future.successful(InternalServerError("Unexpected error with form submission"))
    }
  }

  private def getFormData(credId: String): OptionT[Future, (Seq[String], Seq[String])] = for {
    model      <- businessMatchingService.getModel(credId)
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

  private def saveAndRedirect(formValue: Seq[BusinessActivity], edit: Boolean)(implicit
    request: AuthorisedRequest[AnyContent]
  ): Future[Result] =
    (for {
      model           <- OptionT(
                           dataCacheConnector.fetch[RemoveBusinessTypeFlowModel](request.credId, RemoveBusinessTypeFlowModel.key)
                         ) orElse OptionT.some(RemoveBusinessTypeFlowModel())
      dateApplicable  <- removeBusinessTypeHelper.dateOfChangeApplicable(request.credId, formValue.toSet)
      servicesChanged <-
        OptionT.liftF(Future.successful(model.activitiesToRemove.getOrElse(Set.empty) != formValue.toSet))
      newModel        <- OptionT.liftF(
                           Future.successful(
                             model.copy(
                               activitiesToRemove = Some(formValue.toSet),
                               dateOfChange = if (!dateApplicable || servicesChanged) None else model.dateOfChange
                             )
                           )
                         )
      _               <- OptionT.liftF(dataCacheConnector.save(request.credId, RemoveBusinessTypeFlowModel.key, newModel))
      route           <- OptionT.liftF(router.getRoute(request.credId, WhatBusinessTypesToRemovePageId, newModel, edit))
    } yield route) getOrElse InternalServerError("Post: Cannot retrieve data: RemoveActivitiesController")
}
