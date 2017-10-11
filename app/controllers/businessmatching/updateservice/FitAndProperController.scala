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
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

@Singleton
class FitAndProperController @Inject()(
                                        val authConnector: AuthConnector,
                                        val dataCacheConnector: DataCacheConnector,
                                        val statusService: StatusService)() extends BaseController {

  def get() = Authorised.async {
    implicit request =>
      implicit authContext =>
        statusService.isPreSubmission map {
          case false => Ok(views.html.businessmatching.updateservice.fit_and_proper(EmptyForm))
          case true => NotFound(notFoundView)
        }
  }


  def post() = Authorised.async{
    implicit request => implicit authContext =>
      ???
  }


}