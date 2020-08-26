/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.businessdetails

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.DateOfChange
import models.businessdetails.{BusinessDetails, RegisteredOfficeNonUK, RegisteredOfficeUK}
import org.joda.time.LocalDate
import play.api.mvc.MessagesControllerComponents
import services.StatusService
import utils.{AuthAction, DateOfChangeHelper}
import views.html.date_of_change

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class RegisteredOfficeDateOfChangeController @Inject () (
                                                          val dataCacheConnector: DataCacheConnector,
                                                          val statusService: StatusService,
                                                          val authAction: AuthAction,
                                                          val ds: CommonPlayDependencies,
                                                          val cc: MessagesControllerComponents,
                                                          date_of_change: date_of_change) extends AmlsBaseController(ds, cc) with DateOfChangeHelper {



  def get = authAction.async {
      implicit request =>
        Future(Ok(date_of_change(
          EmptyForm,
          "summary.businessdetails",
          controllers.businessdetails.routes.RegisteredOfficeDateOfChangeController.post()
        )))
    }

  def post = authAction.async {
      implicit request =>
        dataCacheConnector.fetch[BusinessDetails](request.credId, BusinessDetails.key) flatMap { businessDetails =>
          val extraFields: Map[String, Seq[String]] = businessDetails.get.activityStartDate match {
            case Some(date) => Map("activityStartDate" -> Seq(date.startDate.toString("yyyy-MM-dd")))
            case None => Map()
          }
          Form2[DateOfChange](request.body.asFormUrlEncoded.get ++ extraFields) match {
            case form: InvalidForm =>
              Future.successful(BadRequest(date_of_change(
                form, "summary.businessdetails",
                controllers.businessdetails.routes.RegisteredOfficeDateOfChangeController.post())
              ))
            case ValidForm(_, dateOfChange) =>
              for {
                _ <- dataCacheConnector.save[BusinessDetails](request.credId, BusinessDetails.key,
                  businessDetails.registeredOffice(businessDetails.registeredOffice match {
                    case Some(office: RegisteredOfficeUK) => office.copy(dateOfChange = Some(dateOfChange))
                    case Some(office: RegisteredOfficeNonUK) => office.copy(dateOfChange = Some(dateOfChange))
                  }))
              } yield Redirect(routes.SummaryController.get())
          }
        }
  }
}
