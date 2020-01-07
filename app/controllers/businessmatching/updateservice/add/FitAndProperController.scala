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
import connectors.DataCacheConnector
import controllers.DefaultBaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.flowmanagement.{AddBusinessTypeFlowModel, FitAndProperPageId}
import services.flowmanagement.Router
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, BooleanFormReadWrite, RepeatingSection}
import views.html.businessmatching.updateservice.add.fit_and_proper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class FitAndProperController @Inject()(
                                        authAction: AuthAction,
                                        implicit val dataCacheConnector: DataCacheConnector,
                                        val router: Router[AddBusinessTypeFlowModel]
                                      ) extends DefaultBaseController with RepeatingSection {

  val NAME = "passedFitAndProper"

  implicit val boolWrite = BooleanFormReadWrite.formWrites(NAME)
  implicit val boolRead = BooleanFormReadWrite.formRule(NAME, "error.businessmatching.updateservice.fitandproper")

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        getFormData(request.credId) map { case (model) =>
          val form = model.fitAndProper map { v => Form2(v) } getOrElse EmptyForm
          Ok(fit_and_proper(form, edit))
        } getOrElse InternalServerError("Get: Unable to show Fit And Proper page")
  }

  def post(edit: Boolean = false) = authAction.async {
      implicit request =>
        Form2[Boolean](request.body) match {
          case form: InvalidForm => getFormData(request.credId) map { case (_) =>
            BadRequest(fit_and_proper(form, edit))
          } getOrElse InternalServerError("Post: Invalid form on Fit And Proper page")

          case ValidForm(_, data) =>
            dataCacheConnector.update[AddBusinessTypeFlowModel](request.credId, AddBusinessTypeFlowModel.key) {
              case Some(model) => model.isfitAndProper(Some(data)).responsiblePeople(if (data) model.responsiblePeople else None)
            } flatMap {
              case Some(model) => router.getRoute(request.credId, FitAndProperPageId, model, edit)
              case _ => Future.successful(InternalServerError("Post: Cannot retrieve data: FitAndProperController"))
            }
        }
  }

  private def getFormData(credId: String)(implicit hc: HeaderCarrier): OptionT[Future, (AddBusinessTypeFlowModel)] = for {
    model <- OptionT(dataCacheConnector.fetch[AddBusinessTypeFlowModel](credId, AddBusinessTypeFlowModel.key))
  } yield (model)
}