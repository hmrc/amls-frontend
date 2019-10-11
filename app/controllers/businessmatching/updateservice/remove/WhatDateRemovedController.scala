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

package controllers.businessmatching.updateservice.remove

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import jto.validation.Write
import jto.validation.forms.UrlFormEncoded
import models.DateOfChange
import models.flowmanagement.{RemoveBusinessTypeFlowModel, _}
import play.api.mvc.MessagesControllerComponents
import services.flowmanagement.Router
import uk.gov.hmrc.http.HeaderCarrier
import utils.AuthAction
import scala.concurrent.ExecutionContext.Implicits.global
import views.html.date_of_change

import scala.concurrent.Future

@Singleton
class WhatDateRemovedController @Inject()(
                                           authAction: AuthAction,
                                           val ds: CommonPlayDependencies,
                                           val dataCacheConnector: DataCacheConnector,
                                           val router: Router[RemoveBusinessTypeFlowModel],
                                           val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) {

  implicit val dateWrites: Write[DateOfChange, UrlFormEncoded] =
    Write {
      case DateOfChange(b) => Map(
        "dateOfChange.day" -> Seq(b.getDayOfMonth.toString),
        "dateOfChange.month" -> Seq(b.getMonthOfYear.toString),
        "dateOfChange.year" -> Seq(b.getYear.toString)
      )
    }

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      getFormData(request.credId) map { case model =>
        val form = model.dateOfChange map { v => Form2(v) } getOrElse EmptyForm
        Ok(date_of_change(form, "summary.updateservice", routes.WhatDateRemovedController.post(edit), false))
      } getOrElse  InternalServerError("Get: Unable to show date_of_change Activities page. Failed to retrieve data")
    }

  def post(edit: Boolean = false) = authAction.async {
      implicit request =>
        Form2[DateOfChange](request.body) match {
          case form: InvalidForm =>
            Future.successful(BadRequest(date_of_change(form, "summary.updateservice", routes.WhatDateRemovedController.post(edit))))

           case ValidForm(_, data) => dataCacheConnector.update[RemoveBusinessTypeFlowModel](request.credId, RemoveBusinessTypeFlowModel.key) {
              case Some(model) => model.copy(dateOfChange = Some(data))
            } flatMap {
              case Some(model) => router.getRoute(request.credId, WhatDateRemovedPageId, model, edit)
              case _ => Future.successful(InternalServerError("Post: Cannot retrieve data: UpdateServiceDateofChangeController"))
            }
        }
  }

  private def getFormData(credId: String)(implicit hc: HeaderCarrier): OptionT[Future, RemoveBusinessTypeFlowModel] = for {
    model <- OptionT(dataCacheConnector.fetch[RemoveBusinessTypeFlowModel](credId, RemoveBusinessTypeFlowModel.key))
  } yield model

}