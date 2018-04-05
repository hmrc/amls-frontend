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

package services.flowmanagement.routings

import cats.data.OptionT
import connectors.DataCacheConnector
import controllers.businessmatching.updateservice.add.{routes => addRoutes}
import controllers.businessmatching.updateservice.remove.{routes => removeRoutes}
import models.businessmatching.BusinessMatching
import models.businessmatching.updateservice.{ChangeServices, ChangeServicesAdd, ChangeServicesRemove}
import models.flowmanagement.{Flow, FlowMode, PageId}
import play.api.mvc.Result
import play.api.mvc.Results.{InternalServerError, Redirect}
import services.flowmanagement.Router
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object ChangeServicesRouter {

  implicit val router = new Router[ChangeServices] {
    override def getRoute(pageId: PageId, model: ChangeServices, edit: Boolean = false): Future[Result] = model match {
      case ChangeServicesAdd => Future.successful(Redirect(addRoutes.SelectActivitiesController.get()))
      case ChangeServicesRemove => ???
//      case ChangeServicesRemove => {
//        OptionT(getActivities) map { activities =>
//          if (activities.size < 2) {
//            Redirect(removeRoutes.RemoveActivitiesInformationController.get())
//          } else {
//            Redirect(removeRoutes.RemoveActivitiesController.get())
//          }
//        } getOrElse InternalServerError("Unable to show the page")
//      }
    }
  }

//  private def getActivities(implicit dataCacheConnector: DataCacheConnector, hc: HeaderCarrier, ac: AuthContext): Future[Option[Set[String]]] = {
//    dataCacheConnector.fetchAll map {
//      optionalCache =>
//        for {
//          cache <- optionalCache
//          businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
//        } yield businessMatching.activities.fold(Set.empty[String])(_.businessActivities.map(_.getMessage))
//    }
//  }
}
