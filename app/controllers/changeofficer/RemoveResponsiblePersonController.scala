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

package controllers.changeofficer

import javax.inject.Inject
import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import controllers.changeofficer.Helpers._
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.changeofficer._
import models.responsiblepeople.{ResponsiblePerson, ResponsiblePersonEndDate}
import play.api.mvc.MessagesControllerComponents
import utils.{AuthAction, RepeatingSection, StatusConstants}
import scala.concurrent.ExecutionContext.Implicits.global

class RemoveResponsiblePersonController @Inject()(authAction: AuthAction,
                                                  val ds: CommonPlayDependencies,
                                                  implicit val dataCacheConnector: DataCacheConnector,
                                                  val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) with RepeatingSection {

  def get() = authAction.async {
     implicit request => {
      (getNominatedOfficerName(request.credId) map (name =>
        Ok(views.html.changeofficer.remove_responsible_person(EmptyForm, name))
        )) getOrElse InternalServerError("No responsible people found")
    }
  }

  def post() = authAction.async {
     implicit request =>
        Form2[RemovalDate](request.body) match {
          case f: InvalidForm => (getNominatedOfficerName(request.credId) map { name =>
            BadRequest(views.html.changeofficer.remove_responsible_person(f, name))
          }) getOrElse InternalServerError("No responsible people found")
          case ValidForm(_, data) => {
            (for {
              (_, index) <- getNominatedOfficerWithIndex(request.credId)
              _ <- OptionT.liftF(updateDataStrict[ResponsiblePerson](request.credId, index){ responsiblePerson =>
                responsiblePerson.status(StatusConstants.Deleted).copy(endDate = Some(ResponsiblePersonEndDate(data.date)))
              })
            } yield Redirect(routes.NewOfficerController.get())) getOrElse InternalServerError("Cannot update responsible person")
          }
        }
  }
}