/*
 * Copyright 2017 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import models.businessmatching.BusinessMatching
import models.renewal.Renewal
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.summary

import scala.concurrent.Future


@Singleton
class SummaryController @Inject()
(
  val dataCacheConnector: DataCacheConnector,
  val authConnector: AuthConnector,
  val renewalService: RenewalService
) extends BaseController {

  def get = Authorised.async {
    implicit authContext =>
      implicit request =>

        dataCacheConnector.fetchAll flatMap {
          optionalCache =>
            (for {
              cache <- optionalCache
              businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
              renewal <- cache.getEntry[Renewal](Renewal.key)
            } yield {
              Future.successful(Ok(summary(renewal, businessMatching.activities, businessMatching.msbServices)))
            }) getOrElse {
              Future.successful(Redirect(controllers.routes.RegistrationProgressController.get()))
            }
        }
  }
}
