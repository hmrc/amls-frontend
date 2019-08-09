/*
 * Copyright 2019 HM Revenue & Customs
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

package services

import java.net.URLEncoder
import javax.inject.Inject

import config.ApplicationConfig
import models.auth.{CredentialRole, UserDetails}
import models.ReturnLocation
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier

class AuthService @Inject() (authConnector: AuthConnector) {

  private lazy val unauthorisedUrl = URLEncoder.encode(ReturnLocation(controllers.routes.AmlsController.unauthorised_role()).absoluteUrl, "utf-8")
  def signoutUrl = s"${ApplicationConfig.logoutUrl}?continue=$unauthorisedUrl"

  def validateCredentialRole(implicit authContext: AuthContext, headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    authConnector.getUserDetails[UserDetails](authContext) map { result =>
      result.credentialRole match {
        case Some(CredentialRole.User) => true
        case _ => false
      }
    }
}