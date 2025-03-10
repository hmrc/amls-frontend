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

package controllers.supervision

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.supervision.SupervisionStartFormProvider
import models.supervision._

import java.time.LocalDate
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import utils.AuthAction
import views.html.supervision.SupervisionStartView

import javax.inject.Inject
import scala.concurrent.Future

class SupervisionStartController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: SupervisionStartFormProvider,
  view: SupervisionStartView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[Supervision](request.credId, Supervision.key) map {
      case Some(Supervision(anotherBody, _, _, _, _, _)) if getStartDate(anotherBody).isDefined =>
        Ok(view(formProvider().fill(SupervisionStart(getStartDate(anotherBody).get)), edit))
      case _                                                                                    => Ok(view(formProvider(), edit))
    }
  }

  private def getStartDate(anotherBody: Option[AnotherBody]): Option[LocalDate] =
    anotherBody match {
      case Some(body) if body.isInstanceOf[AnotherBodyYes] =>
        body.asInstanceOf[AnotherBodyYes].startDate match {
          case Some(sup) => Option(sup.startDate)
          case _         => None
        }
      case _                                               => None
    }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit))),
        data =>
          dataCacheConnector.fetchAll(request.credId) flatMap { optMap =>
            val result = for {
              cache       <- optMap
              supervision <- cache.getEntry[Supervision](Supervision.key)
              anotherBody <- supervision.anotherBody
            } yield anotherBody match {
              case AnotherBodyYes(_, _, Some(supervisionEndDate), _)
                  if data.startDate.isAfter(supervisionEndDate.endDate) =>
                Future.successful(
                  BadRequest(
                    view(
                      formProvider().withError(
                        "startDate.day",
                        "error.expected.supervision.startdate.before.enddate"
                      ),
                      edit
                    )
                  )
                )
              case _ =>
                dataCacheConnector.save[Supervision](
                  request.credId,
                  Supervision.key,
                  supervision.copy(anotherBody = Some(updateData(anotherBody, data)))
                ) map { _ =>
                  redirect(edit)
                }
            }
            result getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
          }
      )
  }

  private def updateData(anotherBody: AnotherBody, data: SupervisionStart): AnotherBody = {
    val updatedAnotherBody = anotherBody match {
      case a @ AnotherBodyYes(_, _, _, _) => a.startDate(data)
      case _                              => throw new Exception("An UnknownException has occurred : SupervisionStartReasonsController")
    }
    updatedAnotherBody
  }

  private def redirect(edit: Boolean): Result =
    if (edit) {
      Redirect(routes.SummaryController.get())
    } else {
      Redirect(routes.SupervisionEndController.get())
    }
}
