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

package controllers.businessactivities

import javax.inject.Inject

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.{BusinessActivities, TransactionTypes}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessactivities.transaction_types

import scala.concurrent.Future

class TransactionTypesController @Inject()
(
  val authConnector: AuthConnector,
  val cacheConnector: DataCacheConnector
) extends BaseController {

  def get = Authorised.async {
    implicit auth => implicit request => {

      def form(ba: BusinessActivities) = ba.transactionRecordTypes.fold[Form2[TransactionTypes]](EmptyForm)(Form2(_))

      for {
        ba <- OptionT(cacheConnector.fetch[BusinessActivities](BusinessActivities.key))
      } yield Ok(transaction_types(form(ba), edit = false))
    } getOrElse InternalServerError("Cannot fetch business activities")
  }

  def post = Authorised.async {
    implicit auth => implicit request => {
      Form2[TransactionTypes](request.body) match {
        case ValidForm(_, data) => {
          for {
            bm <- OptionT(cacheConnector.fetch[BusinessActivities](BusinessActivities.key))
            _ <- OptionT.liftF(cacheConnector.save[BusinessActivities](BusinessActivities.key, bm.transactionRecordTypes(data)))
          } yield Redirect(routes.IdentifySuspiciousActivityController.get())
        } getOrElse InternalServerError("Unable to update Business Activities Transaction Types")

        case f: InvalidForm =>
          Future.successful(BadRequest(transaction_types(f, edit = false)))
      }
    }
  }

}
