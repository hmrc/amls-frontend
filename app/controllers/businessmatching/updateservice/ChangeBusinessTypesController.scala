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

package controllers.businessmatching.updateservice

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.businessmatching._
import models.businessmatching.updateservice.ChangeBusinessType
import models.flowmanagement.ChangeBusinessTypesPageId
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, RepeatingSection}
import views.html.businessmatching.updateservice._

import scala.collection.immutable.SortedSet
import scala.concurrent.Future

class ChangeBusinessTypesController @Inject()(authAction: AuthAction, val ds: CommonPlayDependencies,
                                              implicit val dataCacheConnector: DataCacheConnector,
                                              val businessMatchingService: BusinessMatchingService,
                                              val router: Router[ChangeBusinessType],
                                              val helper: RemoveBusinessTypeHelper
                                            ) extends AmlsBaseController(ds) with RepeatingSection {

  def get() = authAction.async {
      implicit request =>
        (for {
          (existingActivities, remainingActivities) <- getFormData(request.credId)
        } yield Ok(change_services(EmptyForm, existingActivities, remainingActivities.nonEmpty)))
          .getOrElse(InternalServerError("Unable to show the page"))
  }

  def post() = authAction.async {
      implicit request => {
        Form2[ChangeBusinessType](request.body) match {
          case f: InvalidForm =>
            getFormData(request.credId) map { case (existing, remaining) =>
              BadRequest(change_services(f, existing, remaining.nonEmpty))
            } getOrElse InternalServerError("Unable to show the page")
          case ValidForm(_, data) => {
            for {
              _ <- helper.removeFlowData(request.credId)
              route <- OptionT.liftF(router.getRoute(request.credId, ChangeBusinessTypesPageId, data))
            } yield route
          } getOrElse InternalServerError("Could not remove the flow data")
        }
      }
  }

  private def getFormData(credId: String)(implicit dataCacheConnector: DataCacheConnector, hc: HeaderCarrier) = for {
    cache <- OptionT(dataCacheConnector.fetchAll(credId))
    businessMatching <- OptionT.fromOption[Future](cache.getEntry[BusinessMatching](BusinessMatching.key))
    remainingActivities <- businessMatchingService.getRemainingBusinessActivities(credId)
  } yield {
    val existing = businessMatching.activities.fold(Set.empty[String])(_.businessActivities.map(_.getMessage()))
    val existingSorted = SortedSet[String]() ++ existing
    val remainingActivitiesSorted = SortedSet[String]() ++ remainingActivities

    (existingSorted, remainingActivitiesSorted)
  }

}