/*
 * Copyright 2024 HM Revenue & Customs
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
import forms.businessmatching.MsbSubSectorsFormProvider
import models.businessmatching._
import models.flowmanagement.{AddBusinessTypeFlowModel, SubSectorsPageId}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import utils.AuthAction
import views.html.businessmatching.updateservice.add.MsbSubSectorsView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class SubSectorsController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  implicit val dataCacheConnector: DataCacheConnector,
  val businessMatchingService: BusinessMatchingService,
  val router: Router[AddBusinessTypeFlowModel],
  val config: ApplicationConfig,
  val cc: MessagesControllerComponents,
  formProvider: MsbSubSectorsFormProvider,
  view: MsbSubSectorsView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    (for {
      model <- OptionT(
                 dataCacheConnector.fetch[AddBusinessTypeFlowModel](request.credId, AddBusinessTypeFlowModel.key)
               ) orElse OptionT.some(AddBusinessTypeFlowModel())
    } yield {
      val flowSubServices: Set[BusinessMatchingMsbService] =
        model.subSectors.getOrElse(BusinessMatchingMsbServices(Set())).msbServices
      val form                                             = formProvider().fill(flowSubServices.toSeq)

      Ok(view(form, edit, config.fxEnabledToggle))
    }) getOrElse InternalServerError("Get: Unable to show Sub-Services page. Failed to retrieve data")
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit, config.fxEnabledToggle))),
        data =>
          dataCacheConnector.update[AddBusinessTypeFlowModel](request.credId, AddBusinessTypeFlowModel.key) {
            case Some(model) =>
              model.msbServices(BusinessMatchingMsbServices(data.toSet))
            case _           => throw new Exception("An Unknown Exception has occurred")
          } flatMap {
            case Some(model) => router.getRoute(request.credId, SubSectorsPageId, model, edit)
            case _           => Future.successful(InternalServerError("Post: Cannot retrieve data: SubServicesController"))
          }
      )
  }
}
