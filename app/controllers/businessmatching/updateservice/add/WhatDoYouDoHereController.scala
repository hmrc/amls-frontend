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

package controllers.businessmatching.updateservice.add

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.DefaultBaseController
import controllers.businessmatching.updateservice.AddBusinessTypeHelper
import forms.{Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.businessmatching.{BusinessMatchingMsbService, BusinessMatchingMsbServices}
import models.flowmanagement._
import services.StatusService
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import utils.AuthAction
import views.html.businessmatching.updateservice.add.what_do_you_do_here

import scala.concurrent.Future

@Singleton
class WhatDoYouDoHereController @Inject()(
                                           authAction: AuthAction,
                                           implicit val dataCacheConnector: DataCacheConnector,
                                           val statusService: StatusService,
                                           val businessMatchingService: BusinessMatchingService,
                                           val helper: AddBusinessTypeHelper,
                                           val router: Router[AddBusinessTypeFlowModel]
                                         ) extends DefaultBaseController {

  var msbServiceValues: Set[String] = Set()

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        (for {
          model <- OptionT(dataCacheConnector.fetch[AddBusinessTypeFlowModel](request.credId, AddBusinessTypeFlowModel.key)) orElse OptionT.some(AddBusinessTypeFlowModel())
        } yield {
          val form: Form2[BusinessMatchingMsbServices] = Form2(BusinessMatchingMsbServices(model.tradingPremisesMsbServices.getOrElse(BusinessMatchingMsbServices(Set())).msbServices))
          val flowMsbServices: Set[BusinessMatchingMsbService] = model.subSectors.getOrElse(BusinessMatchingMsbServices(Set())).msbServices
          msbServiceValues = BusinessMatchingMsbServices.all.intersect(flowMsbServices).map(BusinessMatchingMsbServices.getValue)
          Ok(what_do_you_do_here(form, edit, msbServiceValues))
        }) getOrElse InternalServerError("Failed to get subservices")
  }

  def post(edit: Boolean = false) = authAction.async {
     implicit request =>
       import jto.validation.forms.Rules._
        Form2[BusinessMatchingMsbServices](request.body) match {
          case f: InvalidForm => {
            Future.successful(BadRequest(what_do_you_do_here(f, edit, msbServiceValues)))
          }
          case ValidForm(_, data) => {
            dataCacheConnector.update[AddBusinessTypeFlowModel](request.credId, AddBusinessTypeFlowModel.key) {
              case Some(model) =>  model.tradingPremisesMsbServices(data)
            } flatMap {
              case Some(model) => router.getRoute(request.credId, WhatDoYouDoHerePageId, model, edit)
              case _ => Future.successful(InternalServerError("Cannot retrieve data"))
            }
          }
        }
  }
}

