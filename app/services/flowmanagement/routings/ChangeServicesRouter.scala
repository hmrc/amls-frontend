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

import models.businessmatching.updateservice.{ChangeServices, ChangeServicesAdd, ChangeServicesRemove}
import models.flowmanagement.PageId
import play.api.mvc.Result
import services.flowmanagement.Router
import play.api.mvc.Results.Redirect

import scala.concurrent.Future

object ChangeServicesRouter {

  implicit val router = new Router[ChangeServices] {
    override def getRoute(pageId: PageId, model: ChangeServices): Future[Result] = model match {
      case ChangeServicesAdd => Future.successful(Redirect(controllers.businessmatching.updateservice.add.routes.SelectActivitiesController.get()))
      case ChangeServicesRemove => ???
    }
  }

}
