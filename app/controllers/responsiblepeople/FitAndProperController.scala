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

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.responsiblepeople.ResponsiblePeople
import utils.{ControllerHelper, RepeatingSection}

import scala.concurrent.Future

trait FitAndProperController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector
  val FIELDNAME = "hasAlreadyPassedFitAndProper"
  implicit val boolWrite = utils.BooleanFormReadWrite.formWrites(FIELDNAME)
  implicit val boolRead = utils.BooleanFormReadWrite.formRule(FIELDNAME)

  def get(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
      Authorised.async {
        implicit authContext => implicit request =>
          getData[ResponsiblePeople](index) map {
            case Some(ResponsiblePeople(Some(personName),_,_,_,_,_,_,_,_,_,_,_,Some(alreadyPassed),_,_,_,_,_))
              => Ok(views.html.responsiblepeople.fit_and_proper(Form2[Boolean](alreadyPassed), edit, index, fromDeclaration, personName.titleName))
            case Some(ResponsiblePeople(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_))
              => Ok(views.html.responsiblepeople.fit_and_proper(EmptyForm, edit, index, fromDeclaration, personName.titleName))
            case _
              => NotFound(notFoundView)
          }
      }

  def post(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
      Authorised.async {
        implicit authContext => implicit request => {
          Form2[Boolean](request.body) match {
            case f: InvalidForm =>
              getData[ResponsiblePeople](index) map {rp =>
                BadRequest(views.html.responsiblepeople.fit_and_proper(f, edit, index, fromDeclaration, ControllerHelper.rpTitleName(rp)))
              }
            case ValidForm(_, data) =>{
              for {
                result <- updateDataStrict[ResponsiblePeople](index) { rp =>
                  rp.hasAlreadyPassedFitAndProper(data)
                }
              } yield edit match {
                case true => Redirect(routes.DetailedAnswersController.get(index))
                case false => Redirect(routes.PersonRegisteredController.get(index, fromDeclaration))
              }
            }.recoverWith {
              case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
            }
          }
        }
      }

}

object FitAndProperController extends FitAndProperController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
