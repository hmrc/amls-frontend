/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.responsiblepeople.address

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.responsiblepeople.address.NewHomeAddressDateOfChangeFormProvider
import models.responsiblepeople.{NewHomeDateOfChange, ResponsiblePerson}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.address.NewHomeDateOfChangeView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class NewHomeAddressDateOfChangeController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: NewHomeAddressDateOfChangeFormProvider,
  view: NewHomeDateOfChangeView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection {

  def get(index: Int): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetchAll(request.credId) flatMap { cacheMap =>
      (for {
        cache <- cacheMap
        rp    <- getData[ResponsiblePerson](cache, index)
      } yield cache.getEntry[NewHomeDateOfChange](NewHomeDateOfChange.key) match {
        case Some(dateOfChange) =>
          Future.successful(
            Ok(view(formProvider().fill(dateOfChange), index, rp.personName.fold[String]("")(_.fullName)))
          )
        case None               =>
          Future.successful(
            Ok(view(formProvider(), index, rp.personName.fold[String]("")(_.fullName)))
          )
      }).getOrElse(Future.successful(NotFound(notFoundView)))
    }
  }

  def post(index: Int): Action[AnyContent] = authAction.async { implicit request =>
    getPersonName(request.credId, index) flatMap {
      case personName =>
        formProvider()
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(BadRequest(view(formWithErrors, index, personName))),
            data =>
              for {
                _ <- dataCacheConnector.save[NewHomeDateOfChange](request.credId, NewHomeDateOfChange.key, data)
              } yield Redirect(controllers.responsiblepeople.address.routes.NewHomeAddressController.get(index))
          )
      case _          => Future.successful(NotFound(notFoundView))
    }
  }

  private def getPersonName(credId: String, index: Int): Future[String] =
    getData[ResponsiblePerson](credId, index) map { x =>
      val personName = ControllerHelper.rpTitleName(x)
      personName
    }
}
