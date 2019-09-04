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

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.responsiblepeople.{NewHomeDateOfChange, ResponsiblePerson}
import play.api.mvc.{AnyContent, Request}
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.new_home_date_of_change

import scala.concurrent.Future

@Singleton
class NewHomeAddressDateOfChangeController @Inject()(val dataCacheConnector: DataCacheConnector,
                                                     authAction: AuthAction, val ds: CommonPlayDependencies) extends AmlsBaseController(ds) with RepeatingSection {

  def get(index: Int) = authAction.async {
      implicit request =>
        dataCacheConnector.fetchAll(request.credId) flatMap {
          cacheMap =>
            (for {
              cache <- cacheMap
              rp <- getData[ResponsiblePerson](cache, index)
            } yield cache.getEntry[NewHomeDateOfChange](NewHomeDateOfChange.key) match {
              case Some(dateOfChange) => Future.successful(Ok(new_home_date_of_change(Form2(dateOfChange),
                index, rp.personName.fold[String]("")(_.fullName))))
              case None => Future.successful(Ok(new_home_date_of_change(EmptyForm,
                index, rp.personName.fold[String]("")(_.fullName))))
            }).getOrElse(Future.successful(NotFound(notFoundView)))
        }
    }

  def post(index: Int) = authAction.async {
        implicit request =>
          activityStartDateField(request.credId, index) flatMap {
            case (Some(activityStartDate), personName) => {

              val extraFields = Map("activityStartDate" -> Seq(activityStartDate.toString("yyyy-MM-dd")))

              Form2[NewHomeDateOfChange](request.body.asFormUrlEncoded.get ++ extraFields) match {
                case f: InvalidForm =>
                  Future.successful(BadRequest(new_home_date_of_change(f, index, personName)))
                case ValidForm(_, data) => {
                  for {
                    _ <- dataCacheConnector.save[NewHomeDateOfChange](request.credId, NewHomeDateOfChange.key, data)
                  } yield Redirect(routes.NewHomeAddressController.get(index))
                }
              }
            }
            case _ => Future.successful(NotFound(notFoundView))
          }
    }

  private def activityStartDateField(credId: String, index: Int)(implicit request: Request[AnyContent]) = {
    getData[ResponsiblePerson](credId, index) map { x =>
      val startDate = x.flatMap(rp => rp.positions).flatMap(p => p.startDate).map(sd => sd.startDate)
      val personName = ControllerHelper.rpTitleName(x)
      (startDate, personName)
    }
  }
}

