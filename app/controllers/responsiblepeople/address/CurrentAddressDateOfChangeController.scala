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

import cats.data.OptionT
import cats.implicits.catsStdInstancesForFuture
import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.DateOfChangeFormProvider
import models.DateOfChange
import models.responsiblepeople.ResponsiblePerson
import play.api.data.Form
import play.api.mvc._
import services.StatusService
import services.cache.Cache
import utils.{AuthAction, DateOfChangeHelper, RepeatingSection}
import views.html.DateOfChangeView

import scala.concurrent.Future

class CurrentAddressDateOfChangeController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  statusService: StatusService,
  val cc: MessagesControllerComponents,
  formProvider: DateOfChangeFormProvider,
  view: DateOfChangeView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection
    with DateOfChangeHelper {

  def get(index: Int, edit: Boolean): Action[AnyContent] = authAction.async { implicit request =>
    OptionT(getData[ResponsiblePerson](request.credId, index) map {
      case Some(person) =>
        for {
          addressHistory <- person.addressHistory
          currentAddress <- addressHistory.currentAddress
          doc            <- currentAddress.dateOfChange
        } yield formProvider().fill(doc)
      case _            => Some(formProvider())
    }).getOrElse(formProvider()) map { form =>
      Ok(
        view(
          form,
          "summary.responsiblepeople",
          controllers.responsiblepeople.address.routes.CurrentAddressDateOfChangeController.post(index, edit)
        )
      )
    }
  }

  def post(index: Int, edit: Boolean): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => invalidView(formWithErrors, index, edit),
        dateOfChange => validFormView(request.credId, index, dateOfChange, edit)
      )
  }

  private def invalidView(f: Form[DateOfChange], index: Integer, edit: Boolean)(implicit
    request: Request[AnyContent]
  ): Future[Result] =
    Future.successful(
      BadRequest(
        view(
          f,
          "summary.responsiblepeople",
          controllers.responsiblepeople.address.routes.CurrentAddressDateOfChangeController.post(index, edit)
        )
      )
    )

  private def validFormView(credId: String, index: Int, date: DateOfChange, edit: Boolean): Future[Result] =
    doUpdate(credId, index, date).map { cache: Cache =>
      if (cache.getEntry[ResponsiblePerson](ResponsiblePerson.key).exists(_.isComplete)) {
        Redirect(controllers.responsiblepeople.routes.DetailedAnswersController.get(index))
      } else {
        Redirect(routes.TimeAtCurrentAddressController.get(index, edit))
      }
    }

  private def doUpdate(credId: String, index: Int, date: DateOfChange): Future[Cache] =
    updateDataStrict[ResponsiblePerson](credId, index) { res =>
      (for {
        addressHist  <- res.addressHistory
        rpCurrentAdd <- addressHist.currentAddress
      } yield {
        val currentWDateOfChange = rpCurrentAdd.copy(dateOfChange = Some(date))
        val addHistWDateOfChange = addressHist.copy(currentAddress = Some(currentWDateOfChange))
        res.copy(addressHistory = Some(addHistWDateOfChange))
      }).getOrElse(throw new RuntimeException("CurrentAddressDateOfChangeController [post - doUpdate]"))
    }

}
