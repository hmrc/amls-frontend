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

package controllers.businessmatching.updateservice

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.businessmatching._
import models.businessmatching.updateservice.ChangeBusinessType
import models.flowmanagement.ChangeServicesPageId
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection
import views.html.businessmatching.updateservice._

import scala.collection.immutable.SortedSet
import scala.concurrent.Future

class ChangeBusinessTypesController @Inject()(
                                          val authConnector: AuthConnector,
                                          implicit val dataCacheConnector: DataCacheConnector,
                                          val businessMatchingService: BusinessMatchingService,
                                          val router: Router[ChangeBusinessType]
                                        ) extends BaseController with RepeatingSection {

  def get = Authorised.async {
    implicit authContext =>
      implicit request =>
        (for {
          (existingActivities, remainingActivities) <- getFormData
        } yield Ok(change_services(EmptyForm, existingActivities, remainingActivities.nonEmpty)))
          .getOrElse(InternalServerError("Unable to show the page"))
  }

  def post() = Authorised.async {
    implicit authContext =>
      implicit request => {
        Form2[ChangeBusinessType](request.body) match {
          case f: InvalidForm =>
            getFormData map { case (existing, remaining) =>
              BadRequest(change_services(f, existing, remaining.nonEmpty))
            } getOrElse InternalServerError("Unable to show the page")
          case ValidForm(_, data) =>
                router.getRoute(ChangeServicesPageId, data)
        }
      }
  }

  private def getFormData(implicit dataCacheConnector: DataCacheConnector, hc: HeaderCarrier, ac: AuthContext) = for {
    cache <- OptionT(dataCacheConnector.fetchAll)
    businessMatching <- OptionT.fromOption[Future](cache.getEntry[BusinessMatching](BusinessMatching.key))
    remainingActivities <- businessMatchingService.getRemainingBusinessActivities
  } yield {
    val existing = businessMatching.activities.fold(Set.empty[String])(_.businessActivities.map(_.getMessage))
    val existingSorted = SortedSet[String]() ++ existing
    val remainingActivitiesSorted = SortedSet[String]() ++ remainingActivities

    (existingSorted, remainingActivitiesSorted)
  }

}