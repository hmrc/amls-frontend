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

package controllers.msb

import connectors.DataCacheConnector
import controllers.BaseController
import javax.inject.Inject
import models.businessmatching.MoneyServiceBusiness
import models.businessmatching.updateservice.ServiceChangeRegister
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.msb.what_you_need

import scala.concurrent.Future

class WhatYouNeedController @Inject()(val authConnector: AuthConnector,
                                      val statusService: StatusService,
                                      val dataCacheConnector: DataCacheConnector) extends BaseController {

  def get = Authorised.async {
    implicit authContext =>
      implicit request =>
        Future.successful(Ok(what_you_need()))
  }

  def post = Authorised.async {
    implicit authContext =>
      implicit request =>
        dataCacheConnector.fetch[ServiceChangeRegister](ServiceChangeRegister.key) flatMap {
          case Some(register) if register.addedActivities.fold(false)(_.contains(MoneyServiceBusiness)) =>
            Future.successful(Redirect(routes.ExpectedThroughputController.get()))
          case _ =>
            statusService.isPreSubmission map { status =>
              if (status) {
                Redirect(routes.ExpectedThroughputController.get())
              }
              else {
                Redirect(routes.BranchesOrAgentsController.get())
              }
            }
        }
  }
}
