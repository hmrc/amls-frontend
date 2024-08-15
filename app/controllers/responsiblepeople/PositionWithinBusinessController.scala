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

package controllers.responsiblepeople

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.responsiblepeople.PositionWithinBusinessFormProvider
import models.businessmatching.{BusinessMatching, BusinessType}
import models.responsiblepeople._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.PositionWithinBusinessView

import scala.concurrent.Future

class PositionWithinBusinessController @Inject () (
                                                    val dataCacheConnector: DataCacheConnector,
                                                    authAction: AuthAction,
                                                    val ds: CommonPlayDependencies,
                                                    val cc: MessagesControllerComponents,
                                                    formProvider: PositionWithinBusinessFormProvider,
                                                    view: PositionWithinBusinessView,
                                                    implicit val error: views.html.ErrorView) extends AmlsBaseController(ds, cc) with RepeatingSection {



  def get(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request =>
      dataCacheConnector.fetchAll(request.credId) map { optionalCache =>
        (optionalCache flatMap { cache =>

          val bt = ControllerHelper.getBusinessType(cache.getEntry[BusinessMatching](BusinessMatching.key))
            .getOrElse(BusinessType.SoleProprietor)

          val data = cache.getEntry[Seq[ResponsiblePerson]](ResponsiblePerson.key)

          ResponsiblePerson.getResponsiblePersonFromData(data,index) map { person =>

            val positionsInBusiness: Seq[PositionWithinBusiness] = PositionWithinBusiness.buildOptionsList(bt, isDeclaration = false,
              ResponsiblePerson.displayNominatedOfficer(person, ResponsiblePerson.hasNominatedOfficer(data)))

            val positionCheckboxes = positionsInBusiness.map(_.value)

            (person.personName, person.positions) match {
              case (Some(name), Some(p)) => Ok(view(formProvider().fill(p.positions), edit, index, bt, name.titleName,
                ResponsiblePerson.displayNominatedOfficer(person, ResponsiblePerson.hasNominatedOfficer(data)), flow, positionCheckboxes))
              case (Some(name), _) => Ok(view(formProvider(), edit, index, bt, name.titleName,
                ResponsiblePerson.displayNominatedOfficer(person, ResponsiblePerson.hasNominatedOfficer(data)), flow, positionCheckboxes))
              case _ => NotFound(notFoundView)
            }
          }
        }).getOrElse(NotFound(notFoundView))
      }
    }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request =>
      formProvider().bindFromRequest().fold(
        formWithErrors =>
          dataCacheConnector.fetchAll(request.credId) map { optionalCache =>
            (optionalCache map { cache =>

              val bt = ControllerHelper.getBusinessType(cache.getEntry[BusinessMatching](BusinessMatching.key))
                .getOrElse(BusinessType.SoleProprietor)

              val data = cache.getEntry[Seq[ResponsiblePerson]](ResponsiblePerson.key)

              ResponsiblePerson.getResponsiblePersonFromData(data,index) match {
                case s@Some(rp) =>
                  val positionsInBusiness: Seq[PositionWithinBusiness] = PositionWithinBusiness.buildOptionsList(bt, isDeclaration = false,
                    ResponsiblePerson.displayNominatedOfficer(rp, ResponsiblePerson.hasNominatedOfficer(data)))

                  val positionCheckboxes = positionsInBusiness.map(_.value)

                  BadRequest(view(formWithErrors, edit, index, bt, ControllerHelper.rpTitleName(s),
                    ResponsiblePerson.displayNominatedOfficer(rp, ResponsiblePerson.hasNominatedOfficer(data)), flow, positionCheckboxes))
                case None => InternalServerError("Post: An UnknownException has occurred: PositionWithinBusinessController")
              }
            }).getOrElse(NotFound(notFoundView))
          },
        data => {
          for {
            _ <- updateDataStrict[ResponsiblePerson](request.credId, index) { rp => {
                rp.positions match {
                  case Some(x) => rp.positions(Positions.update(x, data))
                  case None => rp.positions(Positions(data, None))
                }
              }
            }
            rpSeqOption <- dataCacheConnector.fetch[Seq[ResponsiblePerson]](request.credId, ResponsiblePerson.key)
          } yield {
                Redirect(routes.PositionWithinBusinessStartDateController.get(index, edit, flow))
          }
        } recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      )
  }
}
