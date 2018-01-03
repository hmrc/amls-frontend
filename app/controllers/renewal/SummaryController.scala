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

package controllers.renewal

import javax.inject.{Inject, Singleton}

import cats.implicits._
import cats.data.OptionT
import connectors.DataCacheConnector
import controllers.BaseController
import forms.EmptyForm
import models.businessmatching.BusinessMatching
import models.renewal.Renewal
import services.{ProgressService, RenewalService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.summary

import scala.concurrent.Future


@Singleton
class SummaryController @Inject()
(
  val dataCacheConnector: DataCacheConnector,
  val authConnector: AuthConnector,
  val renewalService: RenewalService,
  val progressService: ProgressService
) extends BaseController {

  def get = Authorised.async {
    implicit authContext =>
      implicit request =>
        dataCacheConnector.fetchAll flatMap {
          optionalCache =>
            (for {
              cache <- OptionT.fromOption[Future](optionalCache)
              businessMatching <- OptionT.fromOption[Future](cache.getEntry[BusinessMatching](BusinessMatching.key))
              renewal <- OptionT.fromOption[Future](cache.getEntry[Renewal](Renewal.key))
              renewalSection <- OptionT.liftF(renewalService.getSection)
            } yield {
              val variationSections = progressService.sections(cache).filter(_.name != BusinessMatching.messageKey)
              val canSubmit = renewalService.canSubmit(renewalSection, variationSections)
              Ok(summary(EmptyForm, renewal, businessMatching.activities, businessMatching.msbServices, canSubmit))
            }) getOrElse Redirect(controllers.routes.RegistrationProgressController.get())
        }
  }

  def post = Authorised.async {
    implicit authContext => implicit request => (for {
      renewal <- OptionT(dataCacheConnector.fetch[Renewal](Renewal.key))
      _ <- OptionT.liftF(dataCacheConnector.save[Renewal](Renewal.key, renewal.copy(hasAccepted = true)))
    } yield Redirect(controllers.renewal.routes.UpdateAnyInformationController.get)) getOrElse InternalServerError("Could not update renewal")
  }
}
