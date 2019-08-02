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

package services.flowmanagement

import models.flowmanagement.PageId
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{ExecutionContext, Future}

trait Router[A] {
  @deprecated("To be removed when new auth implementation")
  def getRoute(pageId: PageId, model: A, edit: Boolean = false)
              (implicit ac: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): Future[Result]

  def getRouteNewAuth(credId: String, pageId: PageId, model: A, edit: Boolean = false)
              (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Result]
}

