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

package controllers.responsiblepeople

import javax.inject.{Inject, Singleton}

import config.AppConfig
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, _}
import models.responsiblepeople.{ApprovalFlags, ResponsiblePerson}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{ControllerHelper, RepeatingSection}

import scala.concurrent.Future

@Singleton
class FitAndProperController @Inject()(
                                        val dataCacheConnector: DataCacheConnector,
                                        val authConnector: AuthConnector,
                                        appConfig: AppConfig
                                      ) extends RepeatingSection with BaseController {

  val FIELDNAME = "hasAlreadyPassedFitAndProper"
  implicit val boolWrite = utils.BooleanFormReadWrite.formWrites(FIELDNAME)
  implicit val boolRead = utils.BooleanFormReadWrite.formRule(FIELDNAME, "error.required.rp.fit_and_proper")

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = Authorised.async {
    implicit authContext => implicit request =>

      getData[ResponsiblePerson](index) map {
        case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,alreadyPassed,_,_,_,_,_,_)) if (alreadyPassed.hasAlreadyPassedFitAndProper.isDefined) =>
          Ok(views.html.responsiblepeople.fit_and_proper(Form2[Boolean](alreadyPassed.hasAlreadyPassedFitAndProper.get), edit, index, flow, personName.titleName, appConfig.showFeesToggle, appConfig.phase2ChangesToggle))
        case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_)) => {
          Ok(views.html.responsiblepeople.fit_and_proper(EmptyForm, edit, index, flow, personName.titleName, appConfig.showFeesToggle, appConfig.phase2ChangesToggle))
        }
        case _ => NotFound(notFoundView)
      }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) =
    Authorised.async {
      implicit authContext => implicit request =>
        Form2[Boolean](request.body) match {
          case f: InvalidForm =>
            getData[ResponsiblePerson](index) map { rp =>
              BadRequest(views.html.responsiblepeople.fit_and_proper(f, edit, index, flow, ControllerHelper.rpTitleName(rp), appConfig.showFeesToggle, appConfig.phase2ChangesToggle))
            }
          case ValidForm(_, data) => {
            for {
              _ <- updateDataStrict[ResponsiblePerson](index) { rp =>
                rp.approvalFlags(ApprovalFlags(hasAlreadyPassedFitAndProper = Some(data)))
              }
            } yield
              Redirect(routes.DetailedAnswersController.get(index, flow))
          } recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
    }

}