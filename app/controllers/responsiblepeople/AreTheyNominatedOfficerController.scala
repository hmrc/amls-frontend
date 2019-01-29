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
import forms._
import models.responsiblepeople._
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.are_they_nominated_officer

import scala.concurrent.Future

object BooleanFormReadWrite {

  import jto.validation.forms.UrlFormEncoded
  import jto.validation.{From, Rule, Write}
  import utils.MappingUtils.Implicits._
  import jto.validation.forms.Rules._

  def formWrites(fieldName: String): Write[Option[Boolean], UrlFormEncoded] = Write { data: Option[Boolean] => Map(fieldName -> Seq(data.toString)) }

  def formRule(fieldName: String): Rule[UrlFormEncoded, Boolean] = From[UrlFormEncoded] { __ =>
    (__ \ fieldName).read[Boolean].withMessage("error.required.rp.nominated_officer")
  }
}

class AreTheyNominatedOfficerController @Inject () (
                                                     override val dataCacheConnector: DataCacheConnector,
                                                     override val authConnector: AuthConnector
                                                   ) extends RepeatingSection with BaseController {

  val FIELDNAME = "isNominatedOfficer"
  implicit val boolWrite = BooleanFormReadWrite.formWrites(FIELDNAME)
  implicit val boolRead = BooleanFormReadWrite.formRule(FIELDNAME)

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = Authorised.async {
      implicit authContext => implicit request =>
        getData[ResponsiblePerson](index) map { rp =>
          Ok(are_they_nominated_officer(Form2[Option[Boolean]](None), edit, index, flow, ControllerHelper.rpTitleName(rp)))
        }
    }


  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) =
    Authorised.async {
      import jto.validation.forms.Rules._
      implicit authContext => implicit request =>
        Form2[Boolean](request.body) match {
          case f: InvalidForm =>
            getData[ResponsiblePerson](index) map { rp =>
              BadRequest(are_they_nominated_officer(f, edit, index, flow, ControllerHelper.rpTitleName(rp)))
            }
          case ValidForm(_, data) => {

            for {
              _ <- updateDataStrict[ResponsiblePerson](index) { rp =>

                rp.positions match {
                  case Some(pos) if (data & !rp.isNominatedOfficer) =>
                    rp.positions(pos.copy(pos.positions + NominatedOfficer, pos.startDate))
                  case _ => rp
                }
              }
              rpSeqOption <- dataCacheConnector.fetch[Seq[ResponsiblePerson]](ResponsiblePerson.key)
            } yield {
              redirectDependingOnEdit(index, edit, rpSeqOption, flow)(request)
            }

          }.recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
    }

  private def redirectDependingOnEdit(index: Int, edit: Boolean, rpSeqOption: Option[Seq[ResponsiblePerson]],
                                      flow: Option[String])(implicit request: Request[AnyContent]) = {
    rpSeqOption match {
      case Some(rpSeq) => edit match {
        case true => Redirect(routes.DetailedAnswersController.get(index, flow))
        case _ => Redirect(routes.SoleProprietorOfAnotherBusinessController.get(index, edit, flow))
      }
      case _ => NotFound(notFoundView)
    }
  }
}