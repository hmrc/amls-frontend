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
import forms.{Form2, FormHelpers, InvalidForm, ValidForm}
import models.DateOfChange
import models.responsiblepeople.{ResponsiblePerson, ResponsiblePersonAddressHistory, ResponsiblePersonCurrentAddress}
import org.joda.time.LocalDate
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request}
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthAction, DateOfChangeHelper, RepeatingSection}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CurrentAddressDateOfChangeController @Inject()(val dataCacheConnector: DataCacheConnector,
                                                     authAction: AuthAction,
                                                     val ds: CommonPlayDependencies,
                                                     statusService: StatusService,
                                                     val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) with RepeatingSection with DateOfChangeHelper with FormHelpers {

  def get(index: Int, edit: Boolean) = authAction.async {
    implicit request =>
      getData[ResponsiblePerson](request.credId, index) map {
        case Some(ResponsiblePerson(Some(personName), _, _, _, _, _, _, _, _,
        Some(ResponsiblePersonAddressHistory(Some(ResponsiblePersonCurrentAddress(_, _, Some(doc))), _, _)), _, _, _, _, _, _, _, _, _, _, _, _))
        => Ok(views.html.date_of_change(Form2[DateOfChange](DateOfChange(doc.dateOfChange)), "summary.responsiblepeople",
          controllers.responsiblepeople.address.routes.CurrentAddressDateOfChangeController.post(index, edit)
        ))
        case _ => Ok(views.html.date_of_change(Form2[DateOfChange](DateOfChange(LocalDate.now)), "summary.responsiblepeople",
          controllers.responsiblepeople.address.routes.CurrentAddressDateOfChangeController.post(index, edit)
        ))
      }
  }

  def post(index: Int, edit: Boolean) = authAction.async {
    implicit request =>
      val extraInfo = getData[ResponsiblePerson](request.credId, index) map { rpO =>
        for {
          rp <- rpO
          name <- rp.personName
          position <- rp.positions
          date <- position.startDate
        } yield {
          (date, name, rpO)
        }
      }

      extraInfo.flatMap {
        case Some((date, name, responsiblePeople)) => {
          val extraFields = Map("activityStartDate" -> Seq(date.startDate.toString("yyyy-MM-dd")))

          Form2[DateOfChange](request.body.asFormUrlEncoded.get ++ extraFields) match {
            case f: InvalidForm => {
              val fullName = name.fullName
              val dateFormatted = date.startDate.toString("d MMMM yyyy")
              Future.successful(BadRequest(
                views.html.date_of_change(
                  f.withMessageFor(DateOfChange.errorPath, Messages("error.expected.rp.date.after.start", fullName, dateFormatted)),
                  "summary.responsiblepeople",
                  controllers.responsiblepeople.address.routes.CurrentAddressDateOfChangeController.post(index, edit)
                )
              ))
            }
            case ValidForm(_, dateOfChange) => {
              doUpdate(request.credId, index, dateOfChange).map { cache: CacheMap =>
                if (cache.getEntry[ResponsiblePerson](ResponsiblePerson.key).exists(_.isComplete)) {
                  Redirect(controllers.responsiblepeople.routes.DetailedAnswersController.get(index))
                } else {
                  Redirect(routes.TimeAtCurrentAddressController.get(index, edit))
                }
              }
            }
          }
        }
        case _ => Future.successful(NotFound(notFoundView))
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