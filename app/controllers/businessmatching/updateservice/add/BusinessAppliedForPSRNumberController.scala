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

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import javax.inject.{Inject, Singleton}
import models.businessmatching._
import models.flowmanagement.{AddBusinessTypeFlowModel, PsrNumberPageId}
import play.api.mvc.MessagesControllerComponents
import services.flowmanagement.Router
import utils.AuthAction
import scala.concurrent.ExecutionContext.Implicits.global
import views.html.businessmatching.updateservice.add.business_applied_for_psr_number

import scala.concurrent.Future

@Singleton
class BusinessAppliedForPSRNumberController @Inject()(
                                                       authAction: AuthAction,
                                                       val ds: CommonPlayDependencies,
                                                       implicit val dataCacheConnector: DataCacheConnector,
                                                       val router: Router[AddBusinessTypeFlowModel],
                                                       val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        OptionT(dataCacheConnector.fetch[AddBusinessTypeFlowModel](request.credId, AddBusinessTypeFlowModel.key)) map {
          case model if model.isMsbTmDefined =>
          val form = model.businessAppliedForPSRNumber map { v => Form2(v) } getOrElse EmptyForm
          Ok(business_applied_for_psr_number(form, edit))
          case _ => Redirect(controllers.routes.RegistrationProgressController.get())
        } getOrElse InternalServerError("Get: Unable to show Business Applied For PSR Number page")
  }

  def post(edit: Boolean = false) = authAction.async {
      implicit request =>
        Form2[BusinessAppliedForPSRNumber](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(business_applied_for_psr_number(f, edit)))

          case ValidForm(_, data) => {
            dataCacheConnector.update[AddBusinessTypeFlowModel](request.credId, AddBusinessTypeFlowModel.key) {
              case Some(model) => model.businessAppliedForPSRNumber(data)
            } flatMap {
              case Some(model) => router.getRoute(request.credId, PsrNumberPageId, model, edit)
              case _ => Future.successful(InternalServerError("Post: Cannot retrieve data: BusinessAppliedForPSRNumberController"))
            }
          }
        }
  }
}
