/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.businessmatching.updateservice

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.updateservice.{PassedFitAndProper, PassedFitAndProperNo, PassedFitAndProperYes}
import models.responsiblepeople.ResponsiblePeople
import play.api.mvc.{Request, Result}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.RepeatingSection

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global


@Singleton
class FitAndProperController @Inject()(
                                        val authConnector: AuthConnector,
                                        val dataCacheConnector: DataCacheConnector,
                                        val statusService: StatusService)() extends BaseController with RepeatingSection {

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>
        filterPreSubmission {
          Future.successful(Ok(views.html.businessmatching.updateservice.fit_and_proper(EmptyForm)))
        }
  }


  def post() = Authorised.async{
    implicit authContext =>
      implicit request =>
      filterPreSubmission {
        Form2[PassedFitAndProper](request.body) match {
          case ValidForm(_, data) => data match {
            case PassedFitAndProperYes =>
              updateDataStrict[ResponsiblePeople] { responsiblePeople: Seq[ResponsiblePeople] =>
                responsiblePeople.map(_.hasAlreadyPassedFitAndProper(true).copy(hasAccepted = true))
              } map { _ => Redirect(routes.NewServiceInformationController.get()) }
            case PassedFitAndProperNo => Future.successful(Redirect(routes.WhichFitAndProperController.get()))
          }
          case f: InvalidForm => Future.successful(BadRequest(views.html.businessmatching.updateservice.fit_and_proper(f)))
        }
      }
  }

  private def filterPreSubmission(fn: Future[Result])(implicit hc: HeaderCarrier, ac: AuthContext, ec: ExecutionContext, request: Request[_]) = {
    statusService.isPreSubmission flatMap {
      case false => fn
      case true => Future.successful(NotFound(notFoundView))
    }
  }


}