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
import models.businessmatching.{BusinessActivities, BusinessMatching, MoneyServiceBusiness, TrustAndCompanyServices}
import models.responsiblepeople.{PersonResidenceType, ResponsiblePeople, Training}
import play.api.mvc.Result
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{ControllerHelper, RepeatingSection}

import scala.concurrent.Future

trait TrainingController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>
        getData[ResponsiblePeople](index) map {
          case Some(ResponsiblePeople(Some(personName),_,_,_,_,_,_,_,_,_,_,Some(training),_,_,_,_,_,_))
          => Ok(views.html.responsiblepeople.training(Form2[Training](training), edit, index, fromDeclaration, personName.titleName))
          case Some(ResponsiblePeople(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_))
          => Ok(views.html.responsiblepeople.training(EmptyForm, edit, index, fromDeclaration, personName.titleName))
          case _
          => NotFound(notFoundView)
        }
    }

  def post(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request => {
        Form2[Training](request.body) match {
          case f: InvalidForm =>
            getData[ResponsiblePeople](index) map {rp =>
              BadRequest(views.html.responsiblepeople.training(f, edit, index, fromDeclaration, ControllerHelper.rpTitleName(rp)))
            }
          case ValidForm(_, data) => {
            for {
              cacheMap <- fetchAllAndUpdateStrict[ResponsiblePeople](index) { (_, rp) =>
                rp.training(data)
              }
            } yield identifyRoutingTarget(index, edit, cacheMap, fromDeclaration)
          }.recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
      }
    }

  private def identifyRoutingTarget(index: Int, edit: Boolean, cacheMapOpt: Option[CacheMap], fromDeclaration: Boolean): Result = {
    cacheMapOpt match {
      case Some(cacheMap) => {
        (edit, cacheMap.getEntry[BusinessMatching](BusinessMatching.key)) match {
          case (true, _) => Redirect(routes.DetailedAnswersController.get(index))
          case (false, Some(BusinessMatching(_, Some(BusinessActivities(acts)),_,_,_,_, _)))
            if acts.exists(act => act == MoneyServiceBusiness || act == TrustAndCompanyServices)
          => Redirect(routes.FitAndProperController.get(index, false, fromDeclaration))
          case (false, _) => Redirect(routes.PersonRegisteredController.get(index, fromDeclaration))
        }
      }
      case _ => Redirect(routes.PersonRegisteredController.get(index, fromDeclaration))
    }
  }
}

object TrainingController extends TrainingController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
