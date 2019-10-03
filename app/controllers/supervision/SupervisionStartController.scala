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

import org.joda.time.LocalDate
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.supervision._
import play.api.mvc.MessagesControllerComponents
import utils.AuthAction
import views.html.supervision.supervision_start

import scala.concurrent.Future

class SupervisionStartController @Inject()(val dataCacheConnector: DataCacheConnector,
                                           val authAction: AuthAction,
                                           val ds: CommonPlayDependencies,
                                           val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        dataCacheConnector.fetch[Supervision](request.credId, Supervision.key) map {
          case Some(Supervision(anotherBody, _, _, _, _, _)) if getStartDate(anotherBody).isDefined
          => Ok(supervision_start(Form2[SupervisionStart](SupervisionStart(getStartDate(anotherBody).get)), edit))
          case _ => Ok(supervision_start(EmptyForm, edit))
        }
  }

  private def getStartDate(anotherBody: Option[AnotherBody]): Option[LocalDate] = {
    anotherBody match {
      case Some(body) if body.isInstanceOf[AnotherBodyYes] => body.asInstanceOf[AnotherBodyYes].startDate match {
        case Some(sup) => Option(sup.startDate)
        case _ => None
      }
      case _ => None
    }
  }

  def post(edit: Boolean = false) = authAction.async {
      implicit request =>

        dataCacheConnector.fetch[Supervision](request.credId, Supervision.key) flatMap { supervision =>
          def extraFields: Map[String, Seq[String]] = supervision match {
            case Some(s) => getExtraFields(s)
            case _ => Map()
          }

          def getExtraFields(s: Supervision): Map[String, Seq[String]] = {
            s.anotherBody match {
              case Some(data) if data.isInstanceOf[AnotherBodyYes] =>
                Map("extraEndDate" -> Seq(data.asInstanceOf[AnotherBodyYes].endDate match {
                  case Some(date) => date.endDate.toString("yyyy-MM-dd")
                  case None => ""
                }))
              case None => Map()
            }
          }

          Form2[SupervisionStart](request.body.asFormUrlEncoded.get ++ extraFields) match {
            case f: InvalidForm => Future.successful(BadRequest(supervision_start(f, edit)))
            case ValidForm(_, data) =>
              dataCacheConnector.fetchAll(request.credId) flatMap {
                optMap =>
                  val result = for {
                    cache <- optMap
                    supervision <- cache.getEntry[Supervision](Supervision.key)
                    anotherBody <- supervision.anotherBody
                  } yield {
                    dataCacheConnector.save[Supervision](request.credId, Supervision.key,
                      supervision.copy(anotherBody = Some(updateData(anotherBody, data)))) map {
                      _ => redirect(edit)
                    }
                  }
                  result getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
              }
          }
        }
  }

  private def updateData(anotherBody: AnotherBody, data: SupervisionStart): AnotherBody = {
    val updatedAnotherBody = anotherBody match {
      case a@AnotherBodyYes(_, _, _, _) => a.startDate(data)
    }
    updatedAnotherBody
  }

  private def redirect(edit: Boolean) = {
    edit match {
      case true => Redirect(routes.SummaryController.get())
      case false => Redirect(routes.SupervisionEndController.get())
    }
  }
}
