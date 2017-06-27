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

package controllers.changeofficer

import javax.inject.Inject

import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import controllers.changeofficer.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

class RoleInBusinessController @Inject()
(val authConnector: AuthConnector, implicit val dataCacheConnector: DataCacheConnector) extends BaseController {
  def get = Authorised.async {
    implicit authContext => implicit request =>
      val result = for {
        name <- getNominatedOfficerName()
      } yield Ok(name)

      result getOrElse InternalServerError("Unable to get nominated officer")
  }

  def post() = Authorised.async {
    implicit authContext => implicit request =>
      ???
  }
}
