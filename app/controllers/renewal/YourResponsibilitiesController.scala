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

package controllers.renewal

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import models.businessmatching.{BusinessActivities, BusinessMatching, BusinessMatchingMsbServices}
import models.registrationprogress.{NotStarted, Started, TaskRow}
import play.api.Logging
import play.api.mvc._
import services.RenewalService
import utils.AuthAction
import views.html.renewal.YourResponsibilitiesView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class YourResponsibilitiesController @Inject() (
                                                 val dataCacheConnector: DataCacheConnector,
                                                 val authAction: AuthAction,
                                                 val ds: CommonPlayDependencies,
                                                 renewalService: RenewalService,
                                                 val cc: MessagesControllerComponents,
                                                 view: YourResponsibilitiesView
                                               )(implicit executionContext: ExecutionContext)
  extends AmlsBaseController(ds, cc)
    with Logging {

  def get: Action[AnyContent] = authAction.async { implicit request =>
    (for {
      bm      <- OptionT(dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key))
      ba      <- OptionT.fromOption[Future](bm.activities)
      section <- OptionT.liftF(getSection(renewalService, request.credId, Some(ba), bm.msbServices))
    } yield section).getOrElse {
      logger.warn("Unable to retrieve business activities in [renewal][YourResponsibilitiesController]")
      throw new Exception("Unable to retrieve business activities in [renewal][YourResponsibilitiesController]")
    }
  }

  def getSection(
                  renewalService: RenewalService,
                  credId: String,
                  ba: Option[BusinessActivities],
                  msbActivities: Option[BusinessMatchingMsbServices]
                )(implicit request: Request[_]): Future[Result] =
    renewalService.getTaskRow(credId) map {
      case TaskRow(_, _, _, NotStarted | Started, _) => Ok(view(ba, msbActivities))
      case _                                         => Redirect(controllers.routes.RegistrationProgressController.get())
    }
}