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

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.responsiblepeople.{ResponsiblePeople, UKPassport, UKPassportNo, UKPassportYes}
import play.api.i18n.MessagesApi
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.person_uk_passport

import scala.concurrent.Future

class PersonUKPassportController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            val dataCacheConnector: DataCacheConnector,
                                            val authConnector: AuthConnector
                                          ) extends RepeatingSection with BaseController {


  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = Authorised.async {
    implicit authContext =>
      implicit request =>
        getData[ResponsiblePeople](index) map {
          case Some(ResponsiblePeople(Some(personName),_,_,_,_,Some(ukPassport),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_)) =>
            Ok(person_uk_passport(Form2[UKPassport](ukPassport), edit, index, flow, personName.titleName))
          case Some(ResponsiblePeople(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_)) =>
            Ok(person_uk_passport(EmptyForm, edit, index, flow, personName.titleName))
          case _ => NotFound(notFoundView)
        }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[UKPassport](request.body) match {
          case f: InvalidForm => getData[ResponsiblePeople](index) map { rp =>
            BadRequest(person_uk_passport(f, edit, index, flow, ControllerHelper.rpTitleName(rp)))
          }
          case ValidForm(_, data) => {
            (for {
              cache <- OptionT(fetchAllAndUpdateStrict[ResponsiblePeople](index) { (_, rp) =>
                data match {
                  case UKPassportYes(_) if rp.ukPassport.contains(UKPassportNo) => rp.ukPassport(data).copy(nonUKPassport = None)
                  case _ => rp.ukPassport(data)
                }
              })
              rp <- OptionT.fromOption[Future](cache.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key))
            } yield {
              redirectTo(rp, data, index, edit, flow)
            }) getOrElse NotFound(notFoundView)
          } recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
  }

  private def redirectTo(rp: Seq[ResponsiblePeople], data: UKPassport, index: Int, edit: Boolean, flow: Option[String]) = {
    val responsiblePerson = rp(index - 1)
    data match {
      case UKPassportYes(_) if responsiblePerson.dateOfBirth.isEmpty => Redirect(routes.DateOfBirthController.get(index, edit, flow))
      case UKPassportYes(_) if edit => Redirect(routes.DetailedAnswersController.get(index, edit, flow))
      case UKPassportYes(_) => Redirect(routes.DateOfBirthController.get(index, edit, flow))
      case UKPassportNo if edit && responsiblePerson.ukPassport.contains(UKPassportNo) => Redirect(routes.DetailedAnswersController.get(index, edit, flow))
      case UKPassportNo => Redirect(routes.PersonNonUKPassportController.get(index, edit, flow))
    }
  }
}
