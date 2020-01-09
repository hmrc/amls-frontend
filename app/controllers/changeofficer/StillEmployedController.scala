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

import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import controllers.changeofficer.Helpers._
import controllers.changeofficer.routes._
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.changeofficer.{StillEmployed, StillEmployedNo, StillEmployedYes}
import play.api.mvc.MessagesControllerComponents
import utils.AuthAction
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class StillEmployedController @Inject()(authAction: AuthAction,
                                        val ds: CommonPlayDependencies,
                                        implicit val dataCacheConnector: DataCacheConnector,
                                        val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) {

  def get = authAction.async {
     implicit request =>
      (getNominatedOfficerName(request.credId) map (name =>
        Ok(views.html.changeofficer.still_employed(EmptyForm, name))
        )) getOrElse Redirect(NewOfficerController.get())
  }

  def post = authAction.async {
    implicit request =>
      Form2[StillEmployed](request.body) match {
        case x: InvalidForm =>
          (getNominatedOfficerName(request.credId) map (name =>
            BadRequest(views.html.changeofficer.still_employed(x, name))
            )) getOrElse InternalServerError("No responsible people found")
        case ValidForm(_, data) =>
          data match {
            case StillEmployedYes => Future.successful(Redirect(RoleInBusinessController.get()))
            case StillEmployedNo => Future.successful(Redirect(RemoveResponsiblePersonController.get()))
          }
      }
  }
}