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

package controllers.businessmatching.updateservice.add

import cats.data.OptionT
import cats.implicits._

import connectors.DataCacheConnector
import controllers.DefaultBaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.businessmatching.BusinessMatching
import models.flowmanagement.{AddBusinessTypeFlowModel, AddMoreBusinessTypesPageId}
import services.flowmanagement.Router
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, BooleanFormReadWrite}
import views.html.businessmatching.updateservice.add.add_more_activities

import scala.collection.immutable.SortedSet
import scala.concurrent.Future

@Singleton
class AddMoreBusinessTypesController @Inject()(
                                                authAction: AuthAction,
                                                implicit val dataCacheConnector: DataCacheConnector,
                                                val router: Router[AddBusinessTypeFlowModel]
                                           ) extends DefaultBaseController {

  val fieldName = "addmoreactivities"

  implicit val boolWrite = BooleanFormReadWrite.formWrites(fieldName)
  implicit val boolRead = BooleanFormReadWrite.formRule(fieldName, "error.businessmatching.updateservice.addmoreactivities")

  def get() = authAction.async {
      implicit request =>
        (for {
          activities <- OptionT(getActivities(request.credId))
        } yield Ok(add_more_activities(EmptyForm, activities))) getOrElse InternalServerError("Get :Unable to show add more activities page")
  }

  def post() = authAction.async {
      implicit request =>
        Form2[Boolean](request.body) match {
          case f: InvalidForm =>
            OptionT(getActivities(request.credId)) map { activities =>
              BadRequest(add_more_activities(f, activities))
            } getOrElse InternalServerError("Post: Unable to show add more activities page")

          case ValidForm(_, data) =>
            dataCacheConnector.update[AddBusinessTypeFlowModel](request.credId, AddBusinessTypeFlowModel.key) {
              case Some(model) => model.copy(addMoreActivities = Some(data))
            } flatMap {
              case Some(model) => router.getRouteNewAuth(request.credId, AddMoreBusinessTypesPageId, model)
              case _ => Future.successful(InternalServerError("Post: Cannot retrieve data: AddMoreActivitiesController"))
            }
        }
  }

  private def getActivities(credId: String)(implicit dataCacheConnector: DataCacheConnector, hc: HeaderCarrier): Future[Option[Set[String]]] = {
    dataCacheConnector.fetchAll(credId) map {
      optionalCache =>
        for {
          cache <- optionalCache
          businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
        } yield SortedSet[String]() ++ businessMatching.activities.fold(Set.empty[String])(_.businessActivities.map(_.getMessage()))
    }
  }
}