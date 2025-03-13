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
import forms.responsiblepeople.PositionWithinBusinessStartDateFormProvider
import models.businessmatching.{BusinessMatching, BusinessType}
import models.responsiblepeople._
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.PositionWithinBusinessStartDateView

import scala.concurrent.Future

class PositionWithinBusinessStartDateController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: PositionWithinBusinessStartDateFormProvider,
  view: PositionWithinBusinessStartDateView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection
    with Logging {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request =>
      dataCacheConnector.fetchAll(request.credId) map { optionalCache =>
        (optionalCache flatMap { cache =>
          val bt = ControllerHelper
            .getBusinessType(cache.getEntry[BusinessMatching](BusinessMatching.key))
            .getOrElse(BusinessType.SoleProprietor)

          val data = cache.getEntry[Seq[ResponsiblePerson]](ResponsiblePerson.key)

          ResponsiblePerson.getResponsiblePersonFromData(data, index) map { person =>
            (person.personName, person.positions) match {
              case (Some(name), Some(Positions(positions, Some(startDate)))) =>
                Ok(
                  view(
                    formProvider().fill(startDate),
                    edit,
                    index,
                    bt,
                    name.titleName,
                    positions,
                    ResponsiblePerson.displayNominatedOfficer(person, ResponsiblePerson.hasNominatedOfficer(data)),
                    flow
                  )
                )
              case (Some(name), Some(Positions(positions, _)))               =>
                Ok(
                  view(
                    formProvider(),
                    edit,
                    index,
                    bt,
                    name.titleName,
                    positions,
                    ResponsiblePerson.displayNominatedOfficer(person, ResponsiblePerson.hasNominatedOfficer(data)),
                    flow
                  )
                )
              case _                                                         => NotFound(notFoundView)
            }
          }
        }).getOrElse(NotFound(notFoundView))
      }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request =>
      formProvider()
        .bindFromRequest()
        .fold(
          formWithErrors =>
            dataCacheConnector.fetchAll(request.credId) map { optionalCache =>
              (optionalCache map { cache =>
                val bt = ControllerHelper
                  .getBusinessType(cache.getEntry[BusinessMatching](BusinessMatching.key))
                  .getOrElse(BusinessType.SoleProprietor)

                val data = cache.getEntry[Seq[ResponsiblePerson]](ResponsiblePerson.key)

                ResponsiblePerson.getResponsiblePersonFromData(data, index) match {
                  case s @ Some(
                        rp @ ResponsiblePerson(
                          Some(personName),
                          _,
                          _,
                          _,
                          _,
                          _,
                          _,
                          _,
                          _,
                          _,
                          Some(Positions(positions, _)),
                          _,
                          _,
                          _,
                          _,
                          _,
                          _,
                          _,
                          _,
                          _,
                          _,
                          _
                        )
                      ) =>
                    BadRequest(
                      view(
                        formWithErrors,
                        edit,
                        index,
                        bt,
                        ControllerHelper.rpTitleName(s),
                        positions,
                        ResponsiblePerson.displayNominatedOfficer(rp, ResponsiblePerson.hasNominatedOfficer(data)),
                        flow
                      )
                    )
                  case _ => NotFound(notFoundView)
                }
              }).getOrElse(NotFound(notFoundView))
            },
          data =>
            {
              for {
                _           <- updateDataStrict[ResponsiblePerson](request.credId, index) { rp =>
                                 rp.positions match {
                                   case Some(x) => rp.positions(Positions.update(x, data))
                                   case _       =>
                                     val message = "Positions does not exist, cannot update start date"
                                     // $COVERAGE-OFF$
                                     logger.warn(message)
                                     // $COVERAGE-ON$
                                     throw new IllegalStateException(message)
                                 }
                               }
                rpSeqOption <- dataCacheConnector.fetch[Seq[ResponsiblePerson]](request.credId, ResponsiblePerson.key)
              } yield
                if (edit) {
                  Redirect(routes.DetailedAnswersController.get(index, flow))
                } else {
                  Redirect(routes.SoleProprietorOfAnotherBusinessController.get(index, edit, flow))
                }
            } recoverWith { case _: IndexOutOfBoundsException =>
              Future.successful(NotFound(notFoundView))
            }
        )
  }

}
