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
import forms.supervision.SupervisionEndReasonsFormProvider
import models.supervision._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.cache.Cache
import utils.AuthAction
import utils.CharacterCountParser.cleanData
import views.html.supervision.SupervisionEndReasonsView

import javax.inject.Inject
import scala.concurrent.Future

class SupervisionEndReasonsController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: SupervisionEndReasonsFormProvider,
  view: SupervisionEndReasonsView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    val form = formProvider()
    dataCacheConnector.fetch[Supervision](request.credId, Supervision.key) map {
      case Some(Supervision(anotherBody, _, _, _, _, _)) =>
        val param = getEndReasons(anotherBody).fold(form)(x => form.fill(SupervisionEndReasons(x)))
        Ok(view(param, edit))
      case _                                             => Ok(view(form, edit))
    }
  }

  private def getEndReasons(anotherBody: Option[AnotherBody]): Option[String] =
    anotherBody match {
      case Some(body) if body.isInstanceOf[AnotherBodyYes] =>
        body.asInstanceOf[AnotherBodyYes].endingReason match {
          case Some(sup) => Option(sup.endingReason)
          case _         => None
        }
      case _                                               => None
    }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest(cleanData(request.body, "endingReason"))
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit))),
        data =>
          (for {
            supervision <- dataCacheConnector.fetch[Supervision](request.credId, Supervision.key)
            maybeCache  <-
              dataCacheConnector.save[Supervision](request.credId, Supervision.key, updateData(supervision, data))
          } yield maybeCache) map { cache =>
            redirectTo(edit, cache)
          }
      )
  }

  private def updateData(supervision: Supervision, data: SupervisionEndReasons): Supervision = {
    def updatedAnotherBody: AnotherBodyYes = supervision.anotherBody match {
      case Some(ab) => ab.asInstanceOf[AnotherBodyYes].endingReason(data)
      case None     => throw new Exception("An UnknownException has occurred : SupervisionEndReasonsController")
    }

    supervision.anotherBody(updatedAnotherBody).copy(hasAccepted = true)
  }

  private def redirectTo(edit: Boolean, cache: Cache): Result = {
    import utils.ControllerHelper.supervisionComplete

    if (supervisionComplete(cache)) {
      Redirect(routes.SummaryController.get())
    } else {
      Redirect(routes.ProfessionalBodyMemberController.get())
    }
  }
}
