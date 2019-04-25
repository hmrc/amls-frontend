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

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, FormHelpers, InvalidForm, ValidForm}
import models.DateOfChange
import models.responsiblepeople.ResponsiblePerson
import models.responsiblepeople.TimeAtAddress.{SixToElevenMonths, ZeroToFiveMonths}
import org.joda.time.LocalDate
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{DateOfChangeHelper, RepeatingSection}

import scala.concurrent.Future

class CurrentAddressDateOfChangeController @Inject () (
                                                      val dataCacheConnector: DataCacheConnector,
                                                      val authConnector: AuthConnector,
                                                      statusService: StatusService
                                                      ) extends RepeatingSection with BaseController with DateOfChangeHelper with FormHelpers {

  def get(index: Int, edit: Boolean) = Authorised {
    implicit authContext => implicit request =>
      Ok(views.html.date_of_change(
        Form2[DateOfChange](DateOfChange(LocalDate.now)),
        "summary.responsiblepeople",
        controllers.responsiblepeople.routes.CurrentAddressDateOfChangeController.post(index, edit)
      ))
  }

  def post(index: Int, edit: Boolean) = Authorised.async {
    implicit authContext => implicit request =>

      val extraInfo = getData[ResponsiblePerson](index) map { rpO =>
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
                  controllers.responsiblepeople.routes.CurrentAddressDateOfChangeController.post(index, edit)
                )
              ))
            }
            case ValidForm(_, dateOfChange) => {
              val timeAtCurrentO = responsiblePeople flatMap { rp =>
                for {
                  addHist <- rp.addressHistory
                  rpCurr <- addHist.currentAddress
                  timeAtAddress <- rpCurr.timeAtAddress
                } yield timeAtAddress
              }

              doUpdate(index, dateOfChange).map { _ =>
                timeAtCurrentO match {
                  case (Some(ZeroToFiveMonths) | Some(SixToElevenMonths)) if !edit =>
                    Redirect(routes.TimeAtCurrentAddressController.get(index, edit))
                  case Some(_) => Redirect(routes.DetailedAnswersController.get(index))
                }
              }
            }
          }

        }
        case _ => Future.successful(NotFound(notFoundView))
      }
  }

  private def doUpdate
  (index: Int, date: DateOfChange)
  (implicit authContext: AuthContext, request: Request[AnyContent]) =

    updateDataStrict[ResponsiblePerson](index) { res =>
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