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

package controllers.businessmatching.updateservice

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.businessmatching.updateservice.ChangeBusinessTypesFormProvider
import models.businessmatching.updateservice.ChangeBusinessType
import models.flowmanagement.ChangeBusinessTypesPageId
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import utils.{AuthAction, RepeatingSection}
import views.html.businessmatching.updateservice.ChangeServicesView

import javax.inject.Inject
import scala.collection.immutable.SortedSet
import scala.concurrent.Future

class ChangeBusinessTypesController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  implicit val dataCacheConnector: DataCacheConnector,
  val businessMatchingService: BusinessMatchingService,
  val router: Router[ChangeBusinessType],
  val helper: RemoveBusinessTypeHelper,
  val addHelper: AddBusinessTypeHelper,
  val cc: MessagesControllerComponents,
  formProvider: ChangeBusinessTypesFormProvider,
  view: ChangeServicesView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection {

  def get(): Action[AnyContent] = authAction.async { implicit request =>
    (for {
      remainingActivities <- getFormData(request.credId)
    } yield Ok(view(formProvider(), remainingActivities.nonEmpty)))
      .getOrElse(InternalServerError("Unable to show the page"))
  }

  def post(): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors =>
          getFormData(request.credId) map { remaining =>
            BadRequest(view(formWithErrors, remaining.nonEmpty))
          } getOrElse InternalServerError("Unable to show the page"),
        data =>
          {
            for {
              _     <- helper.removeFlowData(request.credId)
              route <- OptionT.liftF(router.getRoute(request.credId, ChangeBusinessTypesPageId, data))
            } yield route
          } getOrElse InternalServerError("Could not remove the flow data")
      )
  }

  private def getFormData(credId: String): OptionT[Future, SortedSet[String]] = for {
    activities         <- businessMatchingService.getRemainingBusinessActivities(credId)
    remainingActivities = activities.map(_.toString)
  } yield {
    val remainingActivitiesSorted = SortedSet[String]() ++ remainingActivities
    remainingActivitiesSorted
  }
}
