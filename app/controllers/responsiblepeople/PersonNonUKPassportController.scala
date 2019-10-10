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

import config.ApplicationConfig
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.responsiblepeople.{NonUKPassport, ResponsiblePerson}
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.person_non_uk_passport

import scala.concurrent.Future

class PersonNonUKPassportController @Inject()(override val messagesApi: MessagesApi,
                                              val dataCacheConnector: DataCacheConnector,
                                              authAction: AuthAction,
                                              val ds: CommonPlayDependencies,
                                              val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) with RepeatingSection {


  def get(index:Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
      implicit request =>
        getData[ResponsiblePerson](request.credId, index) map {
          case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,Some(nonUKPassport),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_)) =>
            Ok(person_non_uk_passport(Form2[NonUKPassport](nonUKPassport), edit, index, flow, personName.titleName))
          case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_)) =>
            Ok(person_non_uk_passport(EmptyForm, edit, index, flow, personName.titleName))
          case _ => NotFound(notFoundView)
        }
  }


  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
      implicit request =>
        Form2[NonUKPassport](request.body) match {
          case f: InvalidForm => getData[ResponsiblePerson](request.credId, index) map { rp =>
            BadRequest(person_non_uk_passport(f, edit, index, flow, ControllerHelper.rpTitleName(rp)))
          }
          case ValidForm(_, data) => {
            for {
              result <- fetchAllAndUpdateStrict[ResponsiblePerson](request.credId, index) { (_, rp) =>
                rp.nonUKPassport(data)
              }
            } yield redirectToNextPage(result, index, edit, flow)

          } recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
  }

  private def redirectToNextPage(result: Option[CacheMap], index: Int, edit: Boolean, flow: Option[String])
                                (implicit request: Request[AnyContent]) = {
    (for {
      cache <- result
      rp <- getData[ResponsiblePerson](cache, index)
    } yield (rp.dateOfBirth.isDefined, edit) match {
      case (true, true) => Redirect(routes.DetailedAnswersController.get(index, flow))
      case (true, false) => Redirect(routes.CountryOfBirthController.get(index, edit, flow))
      case(false, _) => Redirect(routes.DateOfBirthController.get(index, edit, flow))
    }).getOrElse(NotFound(notFoundView))
  }
}
