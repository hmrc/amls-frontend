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
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.supervision._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.supervision.supervision_end_reasons

import scala.concurrent.Future

class SupervisionEndReasonsController @Inject()(val dataCacheConnector: DataCacheConnector,
                                                val authConnector: AuthConnector
                                               ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        dataCacheConnector.fetch[Supervision](Supervision.key) map {
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

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[SupervisionEndReasons](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(supervision_end_reasons(f, edit)))
          case ValidForm(_, data) =>
            for {
              supervision: Option[Supervision] <- dataCacheConnector.fetch[Supervision](Supervision.key)
              _ <- dataCacheConnector.save[Supervision](Supervision.key,
                updateData(supervision, data))
              maybeCache <- dataCacheConnector.fetchAll
              cache <- Future.successful(maybeCache)
            } yield redirectTo(edit, cache)
        }
  }

  private def updateData(supervision: Supervision, data: SupervisionEndReasons): Supervision = {
    def updatedAnotherBody = supervision.anotherBody match {
      case Some(ab) => ab.asInstanceOf[AnotherBodyYes].endingReason(data)
    }

    supervision.anotherBody(updatedAnotherBody).copy(hasAccepted = true)
  }

  private def redirectTo(edit: Boolean, maybeCache: Option[CacheMap])(implicit authContext: AuthContext, headerCarrier: HeaderCarrier) = {
      import utils.ControllerHelper.supervisionComplete

    maybeCache match {
      case Some(cache) => {
        supervisionComplete(cache) match {
          case false => Redirect(routes.ProfessionalBodyMemberController.get())
          case true => Redirect(routes.SummaryController.get())
        }
      }
      case _ => InternalServerError("Could not fetch the data")
    }
  }
}
