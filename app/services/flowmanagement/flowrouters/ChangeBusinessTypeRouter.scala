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

package services.flowmanagement.flowrouters

import controllers.businessmatching.updateservice.add.{routes => addRoutes}
import javax.inject.Inject
import models.businessmatching.updateservice.{ChangeServices, ChangeServicesAdd, ChangeServicesRemove}
import models.flowmanagement.PageId
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import services.flowmanagement.Router
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{ExecutionContext, Future}

class ChangeBusinessTypeRouter @Inject() extends Router[ChangeServices] {
  override def getRoute(pageId: PageId, model: ChangeServices, edit: Boolean = false)
                       (implicit ac: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = model match {

    case ChangeServicesAdd => Future.successful(Redirect(addRoutes.SelectActivitiesController.get()))
    case ChangeServicesRemove => ???
  }
}


