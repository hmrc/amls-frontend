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

package controllers.responsiblepeople.address

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, FormHelpers, InvalidForm, ValidForm}
import models.DateOfChange
import models.responsiblepeople.{ResponsiblePerson, ResponsiblePersonAddressHistory, ResponsiblePersonCurrentAddress}
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request}
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthAction, DateOfChangeHelper, RepeatingSection}
import views.html.date_of_change


import scala.concurrent.Future

class CurrentAddressDateOfChangeController @Inject()(val dataCacheConnector: DataCacheConnector,
                                                     authAction: AuthAction,
                                                     val ds: CommonPlayDependencies,
                                                     statusService: StatusService,
                                                     val cc: MessagesControllerComponents,
                                                     date_of_change: date_of_change) extends AmlsBaseController(ds, cc) with RepeatingSection with DateOfChangeHelper with FormHelpers {

  def get(index: Int, edit: Boolean) = authAction.async {
    implicit request =>
      getData[ResponsiblePerson](request.credId, index) map {
        case Some(ResponsiblePerson(Some(personName), _, _, _, _, _, _, _, _,
        Some(ResponsiblePersonAddressHistory(Some(ResponsiblePersonCurrentAddress(_, _, Some(doc))), _, _)), _, _, _, _, _, _, _, _, _, _, _, _))
        => Ok(date_of_change(Form2[DateOfChange](DateOfChange(doc.dateOfChange)), "summary.responsiblepeople",
          controllers.responsiblepeople.address.routes.CurrentAddressDateOfChangeController.post(index, edit)
        ))
        case _ => Ok(date_of_change(EmptyForm, "summary.responsiblepeople",
          controllers.responsiblepeople.address.routes.CurrentAddressDateOfChangeController.post(index, edit)
        ))
      }
  }

  def post(index: Int, edit: Boolean) = authAction.async {
    implicit request =>
      Form2[DateOfChange](request.body.asFormUrlEncoded.get) match {
        case f: InvalidForm => {
          invalidView(f, index, edit)
        }
        case ValidForm(_, dateOfChange) => {
          validFormView(request.credId, index, dateOfChange, edit)
        }
      }
  }

  private def invalidView(f: forms.Form2[_], index: Integer, edit: Boolean)
                                             (implicit request: Request[AnyContent]) = {
    Future.successful(BadRequest(
      date_of_change(
        f,
        "summary.responsiblepeople",
        controllers.responsiblepeople.address.routes.CurrentAddressDateOfChangeController.post(index, edit)
      )
    ))
  }

  private def validFormView(credId: String, index: Int, date: DateOfChange, edit: Boolean)
                           (implicit request: Request[AnyContent]) = {
    doUpdate(credId, index, date).map { cache: CacheMap =>
      if (cache.getEntry[ResponsiblePerson](ResponsiblePerson.key).exists(_.isComplete)) {
        Redirect(controllers.responsiblepeople.routes.DetailedAnswersController.get(index))
      } else {
        Redirect(routes.TimeAtCurrentAddressController.get(index, edit))
      }
    }
  }

  private def doUpdate(credId: String, index: Int, date: DateOfChange)(implicit request: Request[AnyContent]) =
    updateDataStrict[ResponsiblePerson](credId, index) { res =>
      (for {
        addressHist <- res.addressHistory
        rpCurrentAdd <- addressHist.currentAddress
      } yield {
        val currentWDateOfChange = rpCurrentAdd.copy(dateOfChange = Some(date))
        val addHistWDateOfChange = addressHist.copy(currentAddress = Some(currentWDateOfChange))
        res.copy(addressHistory = Some(addHistWDateOfChange))
      }).getOrElse(throw new RuntimeException("CurrentAddressDateOfChangeController [post - doUpdate]"))
    }

}