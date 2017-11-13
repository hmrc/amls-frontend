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

package controllers.responsiblepeople

import javax.inject.Inject

import config.AppConfig
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.responsiblepeople.ResponsiblePeople
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{ControllerHelper, RepeatingSection}

import scala.concurrent.Future

class FitAndProperController @Inject()(
                                        val dataCacheConnector: DataCacheConnector,
                                        val authConnector: AuthConnector,
                                        config: AppConfig
                                      ) extends RepeatingSection with BaseController {

  val FIELDNAME = "hasAlreadyPassedFitAndProper"
  implicit val boolWrite = utils.BooleanFormReadWrite.formWrites(FIELDNAME)
  implicit val boolRead = utils.BooleanFormReadWrite.formRule(FIELDNAME, "error.required.rp.fit_and_proper")

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = Authorised.async {
        implicit authContext => implicit request =>
          getData[ResponsiblePeople](index) map {
            case Some(ResponsiblePeople(Some(personName),_,_,_,_,_,_,_,_,_,_,_,Some(alreadyPassed),_,_,_,_,_,_)) =>
              Ok(views.html.responsiblepeople.fit_and_proper(Form2[Boolean](alreadyPassed), edit, index, flow, personName.titleName, config.showFeesToggle))
            case Some(ResponsiblePeople(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_)) =>
              Ok(views.html.responsiblepeople.fit_and_proper(EmptyForm, edit, index, flow, personName.titleName, config.showFeesToggle))
            case _ => NotFound(notFoundView)
          }
      }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) =
      Authorised.async {
        implicit authContext => implicit request =>
          Form2[Boolean](request.body) match {
            case f: InvalidForm =>
              getData[ResponsiblePeople](index) map { rp =>
                BadRequest(views.html.responsiblepeople.fit_and_proper(f, edit, index, flow, ControllerHelper.rpTitleName(rp), config.showFeesToggle))
              }
            case ValidForm(_, data) => {
              for {
                _ <- updateDataStrict[ResponsiblePeople](index) { rp =>
                  rp.hasAlreadyPassedFitAndProper(data)
                }
              } yield edit match {
                case true => Redirect(routes.DetailedAnswersController.get(index, edit, flow))
                case false => Redirect(routes.PersonRegisteredController.get(index, flow))
              }
            } recoverWith {
              case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
            }
          }
      }

}