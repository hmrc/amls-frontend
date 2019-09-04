/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.responsiblepeople

import config.AppConfig
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms._
import javax.inject.Inject
import models.responsiblepeople.{ResponsiblePerson, Training}
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import utils.{AuthAction, ControllerHelper, RepeatingSection}

import scala.concurrent.Future

class TrainingController @Inject()(
                                    override val messagesApi: MessagesApi,
                                    val dataCacheConnector: DataCacheConnector,
                                    authAction: AuthAction, val ds: CommonPlayDependencies,
                                    val appConfig: AppConfig
                                  ) extends AmlsBaseController(ds) with RepeatingSection {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) =
    authAction.async {
      implicit request =>
        getData[ResponsiblePerson](request.credId, index) map {
          case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,Some(training),_,_,_,_,_,_,_))
          => Ok(views.html.responsiblepeople.training(Form2[Training](training), edit, index, flow, personName.titleName))
          case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_))
          => Ok(views.html.responsiblepeople.training(EmptyForm, edit, index, flow, personName.titleName))
          case _
          => NotFound(notFoundView)
        }
    }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) =
    authAction.async {
      implicit request => {
        Form2[Training](request.body) match {
          case f: InvalidForm =>
            getData[ResponsiblePerson](request.credId, index) map { rp =>
              BadRequest(views.html.responsiblepeople.training(f, edit, index, flow, ControllerHelper.rpTitleName(rp)))
            }
          case ValidForm(_, data) => {
            for {
              _ <- fetchAllAndUpdateStrict[ResponsiblePerson](request.credId, index) { (_, rp) =>
                rp.training(data)
              }
            } yield identifyRoutingTarget(index, edit, flow)
          }.recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
      }
    }

  private def identifyRoutingTarget(index: Int, edit: Boolean, flow: Option[String]): Result = {
    if (edit) {
      Redirect(routes.DetailedAnswersController.get(index, flow))
    } else {
      Redirect(routes.FitAndProperNoticeController.get(index, edit, flow))
    }
  }
}