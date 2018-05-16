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

import cats.data.OptionT
import cats.implicits._
import controllers.businessmatching.updateservice.add.{routes => addRoutes}
import controllers.businessmatching.updateservice.remove.{routes => removeRoutes}
import javax.inject.{Inject, Singleton}
import models.businessmatching.updateservice.{Add, ChangeBusinessType, Remove}
import models.flowmanagement.PageId
import play.api.mvc.Result
import play.api.mvc.Results.{InternalServerError, Redirect}
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ChangeBusinessTypeRouter @Inject()(val businessMatchingService: BusinessMatchingService
                                        ) extends Router[ChangeBusinessType] {

  override def getRoute(pageId: PageId, model: ChangeBusinessType, edit: Boolean = false)
                       (implicit ac: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = model match {

    case Add => Future.successful(Redirect(addRoutes.SelectBusinessTypeController.get()))
    case Remove => {
      for {
        model <- businessMatchingService.getModel
        activities <- OptionT.fromOption[Future](model.activities) map {
          _.businessActivities
        }
      } yield {
        val desiredResult: Result = {
          if (activities.size < 2) {
            Redirect(removeRoutes.UnableToRemoveBusinessTypesController.get())
          } else {
            Redirect(removeRoutes.RemoveBusinessTypesController.get())
          }
        }
        desiredResult
      }
    } getOrElse InternalServerError("Could not do the get the route for RemoveBusinessTypesSummaryPageRouter")
  }
}
