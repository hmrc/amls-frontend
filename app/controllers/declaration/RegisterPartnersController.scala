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

package controllers.declaration

import javax.inject.{Inject, Singleton}

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import forms.EmptyForm
import models.responsiblepeople.ResponsiblePeople
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.DeclarationHelper._

import scala.concurrent.Future


@Singleton
class RegisterPartnersController @Inject()(val authConnector: AuthConnector,
                                           val dataCacheConnector: DataCacheConnector,
                                           implicit val statusService: StatusService
                                          ) extends BaseController {


  def get() = Authorised.async {
    implicit authContext => implicit request => {

      val result = for {
        subtitle <- OptionT.liftF(statusSubtitle())
        responsiblePeople <- OptionT(dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key))
      } yield {
        Ok(views.html.declaration.register_partners(
          subtitle,
          EmptyForm,
          nonPartners(responsiblePeople),
          currentPartnersNames(responsiblePeople)
        ))
      }
      result getOrElse InternalServerError("failure getting status")
    }
  }

  def post() = Authorised.async {
    implicit authContext => implicit request => {
      Future.successful(Ok(""))
    }
  }
}
