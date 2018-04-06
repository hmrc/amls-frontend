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

package services.flowmanagement

import connectors.DataCacheConnector
import play.api.mvc.Result

import scala.concurrent.Future
import models.flowmanagement.{Edit, Flow, FlowMode, PageId}

trait Router[A] {
  def getRoute(pageId: PageId, model: A, edit: Boolean = false)(implicit dataCacheConnector: DataCacheConnector): Future[Result]
}

//object Router {
//
//  implicit def convertBoolToFlow(edit: Boolean): FlowMode = if (edit) Edit else Flow
//
//  def getRoute[A](pageId: PageId, model: A, edit: Boolean = false)(implicit router: Router[A]) =
//    router.getRoute(pageId, model, edit)
//
//}
