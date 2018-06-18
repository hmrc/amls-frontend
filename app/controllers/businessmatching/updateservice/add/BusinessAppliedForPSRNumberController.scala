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

package controllers.businessmatching.updateservice.add

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import javax.inject.{Inject, Singleton}
import models.businessmatching._
import models.flowmanagement.{AddBusinessTypeFlowModel, PsrNumberPageId}
import services.flowmanagement.Router
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessmatching.updateservice.add.business_applied_for_psr_number

import scala.concurrent.Future

@Singleton
class BusinessAppliedForPSRNumberController @Inject()(
                                                       val authConnector: AuthConnector,
                                                       implicit val dataCacheConnector: DataCacheConnector,
                                                       val router: Router[AddBusinessTypeFlowModel]
                                                     ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        OptionT(dataCacheConnector.fetch[AddBusinessTypeFlowModel](AddBusinessTypeFlowModel.key)) map { case model =>
          val form = model.businessAppliedForPSRNumber map { v => Form2(v) } getOrElse EmptyForm
          Ok(business_applied_for_psr_number(form, edit))
        } getOrElse InternalServerError("Get: Unable to show Business Applied For PSR Number page")
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[BusinessAppliedForPSRNumber](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(business_applied_for_psr_number(f, edit)))

          case ValidForm(_, data) => {
            dataCacheConnector.update[AddBusinessTypeFlowModel](AddBusinessTypeFlowModel.key) {
              case Some(model) => model.businessAppliedForPSRNumber(data)
            } flatMap {
              case Some(model) => router.getRoute(PsrNumberPageId, model, edit)
              case _ => Future.successful(InternalServerError("Post: Cannot retrieve data: BusinessAppliedForPSRNumberController"))
            }
          }
        }
  }
}
