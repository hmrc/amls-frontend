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

import config.AppConfig
import javax.inject.Inject
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.responsiblepeople.{NonUKPassport, ResponsiblePerson}
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.person_non_uk_passport

import scala.concurrent.Future

class PersonNonUKPassportController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            val dataCacheConnector: DataCacheConnector,
                                            val authConnector: AuthConnector,
                                            val appConfig:AppConfig
                                          ) extends RepeatingSection with BaseController {


  def get(index:Int, edit: Boolean = false, flow: Option[String] = None) = Authorised.async {
    implicit authContext =>
      implicit request =>
        getData[ResponsiblePerson](index) map {
          case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,Some(nonUKPassport),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_)) =>
            Ok(person_non_uk_passport(Form2[NonUKPassport](nonUKPassport), edit, index, flow, personName.titleName))
          case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_)) =>
            Ok(person_non_uk_passport(EmptyForm, edit, index, flow, personName.titleName))
          case _ => NotFound(notFoundView)
        }
  }

  private def redirectToNextPage(result: Option[CacheMap], index: Int,
                         edit: Boolean, flow: Option[String] )(implicit authContext:AuthContext, request: Request[AnyContent]) = {
    (for {
      cache <- result
      rp <- getData[ResponsiblePerson](cache, index)
    } yield (rp.dateOfBirth.isDefined && edit) match {
      case true => Redirect(routes.DetailedAnswersController.get(index, flow))
      case false if appConfig.phase2ChangesToggle => Redirect(routes.CountryOfBirthController.get(index, edit, flow))
      case _ => Redirect(routes.DateOfBirthController.get(index, edit, flow))
    }).getOrElse(NotFound(notFoundView))
  }


  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[NonUKPassport](request.body) match {
          case f: InvalidForm => getData[ResponsiblePerson](index) map { rp =>
            BadRequest(person_non_uk_passport(f, edit, index, flow, ControllerHelper.rpTitleName(rp)))
          }
          case ValidForm(_, data) => {
            for {
              result <- fetchAllAndUpdateStrict[ResponsiblePerson](index) { (_, rp) =>
                rp.nonUKPassport(data)
              }
            } yield redirectToNextPage(result, index, edit, flow)

          } recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
  }

}
