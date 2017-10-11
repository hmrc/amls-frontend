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
import forms.EmptyForm
import play.api.mvc.{Request, Result}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global


@Singleton
class FitAndProperController @Inject()(
                                        val authConnector: AuthConnector,
                                        val dataCacheConnector: DataCacheConnector,
                                        val statusService: StatusService)() extends BaseController {

  def get() = Authorised.async {
    implicit request =>
      implicit authContext =>
        filterPreSubmission {
          Future.successful(Ok(views.html.businessmatching.updateservice.fit_and_proper(EmptyForm)))
        }
  }


  def post() = Authorised.async{
    implicit request => implicit authContext =>
      filterPreSubmission {
        Future.successful(Redirect(routes.WhichFitAndProperController.get()))
      }
  }

  private def filterPreSubmission(fn: Future[Result])(implicit hc: HeaderCarrier, ac: AuthContext, ec: ExecutionContext, request: Request[_]) = {
    statusService.isPreSubmission flatMap {
      case false => fn
      case true => Future.successful(NotFound(notFoundView))
    }
  }


}