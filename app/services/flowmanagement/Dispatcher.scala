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

import models.flowmanagement._
import play.api.mvc.Result
import services.flowmanagement.routings.{VariationAddServiceRouting, VariationRemoveServiceRouting}

class Dispatcher() {
  def getRoute[P <: PageId, T <: FlowModel, F <: Flow](pageId: P, model: T, flow: F): Result = flow match {
    case VariationAddServiceFlow => VariationAddServiceRouting.getRoute(pageId.asInstanceOf[AddServiceFlowPageId], model.asInstanceOf[UpdateServiceFlowModel])
    case VariationRemoveServiceFlow => VariationRemoveServiceRouting.getRoute(pageId.asInstanceOf[RemoveServiceFlowPageId], model.asInstanceOf[UpdateServiceFlowModel])
  }
}
