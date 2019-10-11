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

package controllers.supervision

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.supervision._
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthAction
import scala.concurrent.ExecutionContext.Implicits.global
import views.html.supervision.supervision_end_reasons

import scala.concurrent.Future

class SupervisionEndReasonsController @Inject()(val dataCacheConnector: DataCacheConnector,
                                                val authAction: AuthAction,
                                                val ds: CommonPlayDependencies,
                                                val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        dataCacheConnector.fetch[Supervision](request.credId, Supervision.key) map {
          case Some(Supervision(anotherBody, _, _, _, _, _)) if getEndReasons(anotherBody).isDefined
          => Ok(supervision_end_reasons(Form2[SupervisionEndReasons](SupervisionEndReasons(getEndReasons(anotherBody).get)), edit))
          case _ => Ok(supervision_end_reasons(EmptyForm, edit))
        }
  }

  private def getEndReasons(anotherBody: Option[AnotherBody]): Option[String] = {
    anotherBody match {
      case Some(body) if body.isInstanceOf[AnotherBodyYes] => body.asInstanceOf[AnotherBodyYes].endingReason match {
        case Some(sup) => Option(sup.endingReason)
        case _ => None
      }
      case _ => None
    }
  }

  def post(edit: Boolean = false) = authAction.async {
      implicit request =>
        Form2[SupervisionEndReasons](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(supervision_end_reasons(f, edit)))
          case ValidForm(_, data) =>
            (for {
              supervision <- dataCacheConnector.fetch[Supervision](request.credId, Supervision.key)
              maybeCache <- dataCacheConnector.save[Supervision](request.credId, Supervision.key,
                updateData(supervision, data))
            } yield maybeCache) map {
              cache => redirectTo(edit, cache)
            }
        }
  }

  private def updateData(supervision: Supervision, data: SupervisionEndReasons): Supervision = {
    def updatedAnotherBody = supervision.anotherBody match {
      case Some(ab) => ab.asInstanceOf[AnotherBodyYes].endingReason(data)
    }

    supervision.anotherBody(updatedAnotherBody).copy(hasAccepted = true)
  }

  private def redirectTo(edit: Boolean, cache: CacheMap)(implicit headerCarrier: HeaderCarrier) = {
      import utils.ControllerHelper.supervisionComplete

        supervisionComplete(cache) match {
          case false => Redirect(routes.ProfessionalBodyMemberController.get())
          case true => Redirect(routes.SummaryController.get())
        }
    }
}
