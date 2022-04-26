/*
 * Copyright 2022 HM Revenue & Customs
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
import javax.inject.{Inject, Singleton}
import models.businessmatching.{BusinessActivities, BusinessMatching, BusinessMatchingMsbServices}
import models.registrationprogress.{NotStarted, Section, Started}
import play.api.mvc.{MessagesControllerComponents, Request}
import services.RenewalService
import utils.AuthAction
import views.html.renewal._
import play.api.Logging
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class WhatYouNeedController @Inject()(
                                       val dataCacheConnector: DataCacheConnector,
                                       val authAction: AuthAction,
                                       val ds: CommonPlayDependencies,
                                       renewalService: RenewalService,
                                       val cc: MessagesControllerComponents,
                                       what_you_need: what_you_need)(implicit executionContext: ExecutionContext) extends AmlsBaseController(ds, cc) with Logging {

  def get = authAction.async {
    implicit request =>
        (for {
          bm <- OptionT(dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key))
          ba <- OptionT.fromOption[Future](bm.activities)
          section <- OptionT.liftF(getSection(renewalService, request.credId, Some(ba), bm.msbServices))
        } yield {
          section
        }).getOrElse {
          logger.info("Unable to retrieve business activities in [renewal][WhatYouNeedController]")
          throw new Exception("Unable to retrieve business activities in [renewal][WhatYouNeedController]")
        }

  }

  def getSection(renewalService: RenewalService,
                 credId: String, ba: Option[BusinessActivities],
                 msbActivities: Option[BusinessMatchingMsbServices])(implicit request: Request[_]) = {
    renewalService.getSection(credId) map {
      case Section(_, NotStarted | Started, _, _) => Ok(what_you_need(ba, msbActivities))
      case _ => Redirect(controllers.routes.RegistrationProgressController.get)
    }
  }
}