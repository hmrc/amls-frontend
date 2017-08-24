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

package controllers.changeofficer

import javax.inject.Inject

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import controllers.changeofficer.Helpers._
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.changeofficer.{ChangeOfficer, FurtherUpdates, FurtherUpdatesNo, FurtherUpdatesYes}
import models.responsiblepeople.{NominatedOfficer, Positions, ResponsiblePeople}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection

import scala.concurrent.Future

class FurtherUpdatesController @Inject()(
                                          val authConnector: AuthConnector,
                                          implicit val dataCacheConnector: DataCacheConnector
                                        ) extends BaseController with RepeatingSection {

  def get = Authorised.async {
    implicit authContext =>
      implicit request =>
        Future.successful(Ok(views.html.changeofficer.further_updates(EmptyForm)))
  }

  def post = Authorised.async {
    implicit authContext =>
      implicit request => Form2[FurtherUpdates](request.body) match {
        case ValidForm(_, data) => {
          (for {
            cache <- OptionT(dataCacheConnector.fetchAll)
            changeOfficer <- OptionT.fromOption[Future](cache.getEntry[ChangeOfficer](ChangeOfficer.key))
            newOfficer <- OptionT.fromOption[Future](changeOfficer.newOfficer)
            responsiblePeople <- OptionT.fromOption[Future](cache.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key))
            (_, index) <- OptionT.fromOption[Future](matchNominatedOfficerWithResponsiblePerson(newOfficer, responsiblePeople))
            _ <- OptionT.liftF(updateDataStrict[ResponsiblePeople](index + 1){ p =>
              val positions = p.positions.get
              p.positions(
                Positions(positions.positions + NominatedOfficer, positions.startDate)
              )
            })
          } yield {
            Redirect(
              data match {
                case FurtherUpdatesYes => controllers.routes.RegistrationProgressController.get()
                case FurtherUpdatesNo => controllers.declaration.routes.WhoIsRegisteringController.get()
              })
          }) getOrElse InternalServerError("Cannot save new Nominated Officer")
        }
        case f: InvalidForm => Future.successful(BadRequest(views.html.changeofficer.further_updates(f)))
      }
  }

}
