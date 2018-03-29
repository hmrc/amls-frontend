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

package controllers.businessmatching.updateservice

import cats.data.OptionT
import cats.implicits._
import config.AppConfig
import connectors.DataCacheConnector
import controllers.BaseController
import controllers.businessmatching.updateservice.routes._
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.businessmatching.{MoneyServiceBusiness, TrustAndCompanyServices}
import models.responsiblepeople.ResponsiblePeople
import play.api.mvc.{Request, Result}
import services.StatusService
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{BooleanFormReadWrite, RepeatingSection}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpdateServicesSummaryController @Inject()(
                                                 val authConnector: AuthConnector,
                                                 val dataCacheConnector: DataCacheConnector
                                               ) extends BaseController with RepeatingSection {

  def get() = Authorised.async {
    implicit authContext =>
      implicit request => ???
  }

  def post() = Authorised.async{
    implicit authContext =>
      implicit request =>  ???

  }


}