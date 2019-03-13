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
import org.joda.time.LocalDate
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.supervision.supervision_end

import scala.concurrent.Future

class SupervisionEndController @Inject()(val dataCacheConnector: DataCacheConnector,
                                         val authConnector: AuthConnector
                                      ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[Supervision](Supervision.key) map {
        case Some(Supervision(anotherBody, _, _, _, _, _)) if getEndDate(anotherBody).isDefined
        => Ok(supervision_end(Form2[SupervisionEnd](SupervisionEnd(getEndDate(anotherBody).get)), edit))
        case _ => Ok(supervision_end(EmptyForm, edit))
      }
  }

  private def getEndDate(anotherBody: Option[AnotherBody]): Option[LocalDate] = {
    anotherBody match {
      case Some(body) if body.isInstanceOf[AnotherBodyYes] => body.asInstanceOf[AnotherBodyYes].endDate match {
        case Some(sup) => Option(sup.endDate)
        case _ => None
      }
      case _ => None
    }
  }

  def post(edit : Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>

      dataCacheConnector.fetch[Supervision](Supervision.key) flatMap { supervision =>
        def extraFields: Map[String, Seq[String]] = supervision match {
          case Some(s) => getExtraFields(s)
          case _ => Map()
        }

        def getExtraFields(s: Supervision): Map[String, Seq[String]] = {
          s.anotherBody match {
            case Some(data) if data.isInstanceOf[AnotherBodyYes] =>
              Map("extraStartDate" -> Seq(data.asInstanceOf[AnotherBodyYes].startDate.get.startDate.toString("yyyy-MM-dd")))
            case None => Map()
          }
        }

        Form2[SupervisionEnd](request.body.asFormUrlEncoded.get ++ extraFields) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(supervision_end(f, edit)))
          case ValidForm(_, data) =>
            dataCacheConnector.fetchAll flatMap {
              optMap =>
                val result = for {
                  cache <- optMap
                  supervision <- cache.getEntry[Supervision](Supervision.key)
                  anotherBody <- supervision.anotherBody
                } yield {
                  dataCacheConnector.save[Supervision](Supervision.key,
                    supervision.copy(anotherBody = Some(updateData(anotherBody, data)))) map {
                    _ => redirect(edit)
                  }
                }
                result getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
            }
        }
      }
  }

  private def updateData(anotherBody: AnotherBody, data: SupervisionEnd): AnotherBody = {
    val updatedAnotherBody = anotherBody match {
      case a@AnotherBodyYes(_, _, _, _) => a.endDate(data)
    }
    updatedAnotherBody
  }

  private def redirect(edit: Boolean) = {
    edit match {
      case true => Redirect(routes.SummaryController.get())
      case false => Redirect(routes.SupervisionEndReasonsController.get(false))
    }
  }
}
