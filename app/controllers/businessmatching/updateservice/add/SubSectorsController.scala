/*
 * Copyright 2020 HM Revenue & Customs
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
import config.ApplicationConfig
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.businessmatching._
import models.flowmanagement.{AddBusinessTypeFlowModel, SubSectorsPageId}
import play.api.mvc.MessagesControllerComponents
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import utils.AuthAction
import scala.concurrent.ExecutionContext.Implicits.global
import views.html.businessmatching.updateservice.add.msb_subservices

import scala.concurrent.Future


@Singleton
class SubSectorsController @Inject()(authAction: AuthAction,
                                     val ds: CommonPlayDependencies,
                                     implicit val dataCacheConnector: DataCacheConnector,
                                     val businessMatchingService: BusinessMatchingService,
                                     val router: Router[AddBusinessTypeFlowModel],
                                     val config:ApplicationConfig,
                                     val cc: MessagesControllerComponents,
                                     msb_subservices: msb_subservices) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        (for {
          model <- OptionT(dataCacheConnector.fetch[AddBusinessTypeFlowModel](request.credId, AddBusinessTypeFlowModel.key)) orElse OptionT.some(AddBusinessTypeFlowModel())
        } yield {
          val flowSubServices: Set[BusinessMatchingMsbService] = model.subSectors.getOrElse(BusinessMatchingMsbServices(Set())).msbServices
          val form: Form2[BusinessMatchingMsbServices] = Form2(BusinessMatchingMsbServices(flowSubServices))

          Ok(msb_subservices(form, edit, config.fxEnabledToggle))
        }) getOrElse InternalServerError("Get: Unable to show Sub-Services page. Failed to retrieve data")
  }

  def post(edit: Boolean = false) = authAction.async {
      implicit request =>
        import jto.validation.forms.Rules._
        Form2[BusinessMatchingMsbServices](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(msb_subservices(f, edit, config.fxEnabledToggle)))
          case ValidForm(_, data) => {
            dataCacheConnector.update[AddBusinessTypeFlowModel](request.credId, AddBusinessTypeFlowModel.key) {
              case Some(model) => {
                model.msbServices(data)
              }
            } flatMap {
              case Some(model) => router.getRoute(request.credId, SubSectorsPageId, model, edit)
              case _ => Future.successful(InternalServerError("Post: Cannot retrieve data: SubServicesController"))
            }
          }
        }
  }
}
