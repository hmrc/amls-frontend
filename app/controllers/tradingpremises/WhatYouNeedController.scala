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

package controllers.tradingpremises


import connectors.DataCacheConnector
import controllers.BaseController
import javax.inject.{Inject, Singleton}
import models.businessmatching.BusinessMatching
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper
import views.html.tradingpremises._

@Singleton
class WhatYouNeedController @Inject()(val dataCacheConnector: DataCacheConnector,
                                      val authConnector: AuthConnector) extends BaseController {

  def get(index: Int) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key) map {
        response =>
        Ok(what_you_need(index, ControllerHelper.isMSBSelected(response)))
      }
  }

}
